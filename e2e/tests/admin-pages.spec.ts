import { test, expect } from '@playwright/test';

const pages: [string, string][] = [
  ['/admin', 'Welcome'],
  ['/admin/students', 'Students'],
  ['/admin/teachers', 'Teachers'],
  ['/admin/classes', 'Classes'],
  ['/admin/finance/payments', 'Fee payments'],
  ['/admin/finance/dues', 'Pending and overdue fees'],
  ['/admin/finance/fee-structure', 'Fee structure'],
  ['/admin/finance/salaries', 'Teacher salaries'],
  ['/admin/finance/expenses', 'Expenses'],
  ['/admin/finance/reports', 'Financial reports'],
  ['/admin/content/settings', 'Page content'],
  ['/admin/content/facilities', 'Facilities'],
  ['/admin/content/gallery', 'Photo gallery'],
  ['/admin/content/announcements', 'Announcements'],
  ['/admin/content/testimonials', 'Testimonials'],
  ['/admin/content/management', 'Management team'],
  ['/admin/content/enquiries', 'Admission enquiries'],
  ['/admin/users', 'Staff accounts'],
  ['/admin/activity', 'Activity log'],
  ['/admin/account', 'My account'],
];

for (const [path, title] of pages) {
  test(`${path} opens without errors`, async ({ page }) => {
    const problems: string[] = [];
    page.on('pageerror', (e) => problems.push(e.message));
    page.on('console', (m) => m.type() === 'error' && problems.push(m.text()));
    page.on('response', (r) => r.status() >= 400 && !r.url().includes('/_next/') && problems.push(`${r.status()} ${r.url()}`));
    await page.goto(path);
    await expect(page.getByRole('heading', { level: 1 })).toContainText(title);
    await page.waitForLoadState('networkidle');
    expect(problems).toEqual([]);
  });
}

test('dashboard shows the four headline numbers and charts', async ({ page }) => {
  await page.goto('/admin');
  for (const label of ['Students', 'Teachers', 'Fees collected (this year)', 'Fees pending (this year)']) {
    await expect(page.getByText(label, { exact: true }).first()).toBeVisible();
  }
  await expect(page.locator('.recharts-wrapper').first()).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Students in each class' })).toBeVisible();
});

test('sidebar marks the current page and works on a phone', async ({ page }) => {
  await page.goto('/admin/teachers');
  await expect(page.getByRole('navigation', { name: 'Admin' }).getByRole('link', { name: 'Teachers' })).toHaveAttribute('aria-current', 'page');

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/admin');
  await expect(page.getByRole('navigation', { name: 'Admin' })).toBeHidden();
  await page.getByRole('button', { name: 'Open menu' }).click();
  await page.getByRole('navigation', { name: 'Admin' }).getByRole('link', { name: 'Students' }).click();
  await expect(page).toHaveURL(/\/admin\/students$/);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  expect(overflow).toBe(false);
});

test('reports can be downloaded as Excel and PDF', async ({ page }) => {
  await page.goto('/admin/finance/reports');
  await expect(page.getByText('Fees collected', { exact: true }).first()).toBeVisible();
  const [xlsx] = await Promise.all([page.waitForEvent('download'), page.getByRole('link', { name: 'Excel' }).click()]);
  expect(xlsx.suggestedFilename()).toMatch(/^finance-report-\d{4}-\d{2}\.xlsx$/);
  const [pdf] = await Promise.all([page.waitForEvent('download'), page.getByRole('link', { name: 'PDF' }).click()]);
  expect(pdf.suggestedFilename()).toMatch(/\.pdf$/);
});

test('pending fees list can be filtered and exported', async ({ page }) => {
  await page.goto('/admin/finance/dues');
  await expect(page.locator('tbody tr').first()).toBeVisible();
  const before = await page.locator('tbody tr').count();
  await page.getByLabel('Class', { exact: true }).selectOption({ label: 'Nursery' });
  await expect.poll(async () => page.locator('tbody tr').count()).toBeLessThan(before);
  const [csv] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Download list' }).click()]);
  expect(csv.suggestedFilename()).toMatch(/^pending-fees-.*\.csv$/);
});
