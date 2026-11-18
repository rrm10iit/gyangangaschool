import { expect, test, type Browser } from '@playwright/test';
import { alert } from './helpers';

test.describe.configure({ mode: 'serial' });

const stamp = Date.now().toString().slice(-7);
const accounts = {
  teacher: { email: `e2e.teacher.${stamp}@example.com`, name: 'E2E Teacher', role: 'TEACHER', password: 'TeacherPass-123' },
  accountant: { email: `e2e.accountant.${stamp}@example.com`, name: 'E2E Accountant', role: 'ACCOUNTANT', password: 'AccountantPass-123' },
};

test.beforeAll(async ({ request }) => {
  for (const a of Object.values(accounts)) {
    const res = await request.post('/api/admin/users', {
      data: { email: a.email, fullName: a.name, role: a.role, enabled: true, password: a.password },
    });
    expect(res.status(), await res.text()).toBe(200);
  }
});

test.afterAll(async ({ request }) => {
  const list = await (await request.get(`/api/admin/users?q=e2e.&size=50`)).json();
  for (const u of list.content) await request.delete(`/api/admin/users/${u.id}`);
});

async function signedIn(browser: Browser, who: keyof typeof accounts) {
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto('/admin/login');
  await page.getByLabel('Email').fill(accounts[who].email);
  await page.getByLabel('Password').fill(accounts[who].password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/admin$/);
  return { ctx, page };
}

test('a teacher sees only the school section, can read students but not change them or see money', async ({ browser }) => {
  const { ctx, page } = await signedIn(browser, 'teacher');
  const nav = page.getByRole('navigation', { name: 'Admin' });
  await expect(nav.getByRole('link', { name: 'Students' })).toBeVisible();
  await expect(nav.getByRole('link', { name: 'Fee payments' })).toHaveCount(0);
  await expect(nav.getByRole('link', { name: 'Page content' })).toHaveCount(0);
  await expect(nav.getByRole('link', { name: 'Staff accounts' })).toHaveCount(0);

  await page.getByRole('link', { name: 'Students' }).click();
  await expect(page.locator('tbody tr').first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add student' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Edit' })).toHaveCount(0);

  // the dashboard has no money in it
  await page.goto('/admin');
  await expect(page.getByText('Fees collected (this year)')).toHaveCount(0);

  // and the server agrees, whatever the screen shows
  expect((await page.request.get('/api/admin/finance/dues')).status()).toBe(403);
  expect((await page.request.get('/api/admin/users')).status()).toBe(403);
  expect((await page.request.post('/api/admin/students', { data: { fullName: 'x', admissionNo: 'x', classId: 1 } })).status()).toBe(403);
  await ctx.close();
});

test('typing a finance URL as a teacher shows no money data', async ({ browser }) => {
  const { ctx, page } = await signedIn(browser, 'teacher');
  await page.goto('/admin/finance/payments');
  await expect(page.getByText(/Request failed|permission/i).first()).toBeVisible();
  await expect(page.locator('tbody tr', { hasText: '₹' })).toHaveCount(0);
  await ctx.close();
});

test('an accountant handles finance but cannot edit students or the website', async ({ browser }) => {
  const { ctx, page } = await signedIn(browser, 'accountant');
  const nav = page.getByRole('navigation', { name: 'Admin' });
  await expect(nav.getByRole('link', { name: 'Fee payments' })).toBeVisible();
  await expect(nav.getByRole('link', { name: 'Reports' })).toBeVisible();
  await expect(nav.getByRole('link', { name: 'Page content' })).toHaveCount(0);

  await page.goto('/admin/finance/payments');
  await expect(page.locator('tbody tr').first()).toBeVisible();
  // can record but not cancel a receipt
  await expect(page.getByRole('button', { name: 'Record payment' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Delete' })).toHaveCount(0);

  await page.goto('/admin/students');
  await expect(page.getByRole('button', { name: 'Add student' })).toHaveCount(0);
  expect((await page.request.get('/api/admin/settings')).status()).toBe(403);
  await ctx.close();
});

test('an account that is switched off cannot get in', async ({ browser, request }) => {
  const list = await (await request.get('/api/admin/users?q=e2e.teacher&size=5')).json();
  const u = list.content[0];
  const off = await request.put(`/api/admin/users/${u.id}`, {
    data: { email: u.email, fullName: u.fullName, role: u.role, enabled: false },
  });
  expect(off.status()).toBe(200);
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto('/admin/login');
  await page.getByLabel('Email').fill(accounts.teacher.email);
  await page.getByLabel('Password').fill(accounts.teacher.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(alert(page)).toHaveText('Incorrect email or password');
  await ctx.close();
});
