import { defineConfig, devices } from '@playwright/test';

/**
 * Environment:
 *   E2E_BASE_URL        where the stack listens (default http://localhost)
 *   E2E_ADMIN_EMAIL     admin account (default admin@gyanganga.school)
 *   E2E_ADMIN_PASSWORD  its password (required)
 *   E2E_CHANNEL         use an installed browser, e.g. "msedge" or "chrome". Empty = Playwright's Chromium.
 *
 * The stack must be started with SEED_DEMO_DATA=true so there are classes, students and photos to test with.
 */
export const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost';
export const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL || 'admin@gyanganga.school';
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD || '';

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    channel: process.env.E2E_CHANNEL || undefined,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'public',
      testMatch: /public\.spec\.ts|a11y\.spec\.ts|security\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL || undefined },
    },
    {
      name: 'admin',
      testMatch: /admin-.*\.spec\.ts/,
      dependencies: ['setup'],
      use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL || undefined, storageState: '.auth/admin.json' },
    },
    {
      name: 'roles',
      testMatch: /roles\.spec\.ts/,
      dependencies: ['setup'],
      use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL || undefined, storageState: '.auth/admin.json' },
    },
  ],
});
