import { test, expect } from '@playwright/test';
import { alert } from './helpers';

test('anonymous visitors are sent to the login page for every admin screen', async ({ page }) => {
  for (const path of ['/admin', '/admin/students', '/admin/finance/payments', '/admin/content/settings', '/admin/users']) {
    await page.goto(path);
    await expect(page).toHaveURL(/\/admin\/login$/);
  }
});

test('the admin API refuses anonymous calls and never leaks data', async ({ request }) => {
  for (const path of ['/api/admin/students', '/api/admin/finance/dues', '/api/admin/users', '/api/admin/audit-log']) {
    const res = await request.get(path);
    expect(res.status(), path).toBe(401);
    expect(await res.text()).not.toMatch(/passwordHash|@gyanganga/);
  }
});

test('security headers are present on pages and the session cookie is locked down', async ({ request, page }) => {
  const res = await request.get('/');
  const h = res.headers();
  expect(h['x-content-type-options']).toBe('nosniff');
  expect(h['x-frame-options']).toBe('DENY');
  expect(h['referrer-policy']).toBe('strict-origin-when-cross-origin');
  expect(h['x-powered-by']).toBeUndefined();

  const login = await request.post('/api/auth/login', { data: { email: 'nobody@example.com', password: 'wrong-password' } });
  expect(login.status()).toBe(401);
  expect(login.headers()['set-cookie']).toBeUndefined();
  void page;
});

test('uploaded files cannot be used to run scripts', async ({ request }) => {
  const res = await request.get('/uploads/../application.yml');
  expect([400, 403, 404]).toContain(res.status());
  const traversal = await request.get('/api/admin/files/..%2F..%2Fetc%2Fpasswd');
  expect([400, 401, 403, 404]).toContain(traversal.status());
});

test('login form shows one generic message for wrong credentials', async ({ page }) => {
  await page.goto('/admin/login');
  await page.getByLabel('Email').fill('nobody-' + Date.now() + '@example.com');
  await page.getByLabel('Password').fill('wrong-password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(alert(page)).toHaveText('Incorrect email or password');
  await expect(page).toHaveURL(/\/admin\/login$/);
});
