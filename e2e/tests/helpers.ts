import type { Page } from '@playwright/test';

/** Visible error/status banners. Excludes Next.js's own invisible route announcer, which is also role=alert. */
export const alert = (page: Page) => page.locator('[role=alert]:not(#__next-route-announcer__)');

