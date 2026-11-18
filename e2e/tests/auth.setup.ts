import { test as setup, expect } from '@playwright/test';
import { ADMIN_EMAIL, ADMIN_PASSWORD } from '../playwright.config';

setup('sign in as admin once and reuse the session', async ({ page }) => {
  expect(ADMIN_PASSWORD, 'Set E2E_ADMIN_PASSWORD').not.toBe('');
  await page.goto('/admin/login');
  await page.getByLabel('Email').fill(ADMIN_EMAIL);
  await page.getByLabel('Password').fill(ADMIN_PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/admin$/);
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible();
  await page.context().storageState({ path: '.auth/admin.json' });
});
