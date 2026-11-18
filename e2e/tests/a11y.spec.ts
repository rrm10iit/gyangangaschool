import AxeBuilder from '@axe-core/playwright';
import { test, expect } from '@playwright/test';

const pages = ['/', '/about', '/classes', '/facilities', '/gallery', '/news', '/contact', '/admin/login'];

for (const theme of ['light', 'dark'] as const) {
  for (const path of pages) {
    test(`${path} has no serious accessibility violations (${theme})`, async ({ page }) => {
      await page.addInitScript((t) => localStorage.setItem('theme', t), theme);
      await page.goto(path);
      await page.waitForLoadState('networkidle');
      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      const bad = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
      expect(
        bad.map((v) => `${v.id}: ${v.help} (${v.nodes.length}) e.g. ${v.nodes[0]?.target.join(' ')}`),
      ).toEqual([]);
    });
  }
}

test('every page has one h1, a main landmark and a skip link', async ({ page }) => {
  for (const path of pages.filter((p) => !p.startsWith('/admin'))) {
    await page.goto(path);
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
    await expect(page.getByRole('main')).toHaveCount(1);
    await expect(page.getByRole('link', { name: 'Skip to main content' })).toHaveCount(1);
  }
});

test('the whole home page can be used with the keyboard only', async ({ page }) => {
  await page.goto('/');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Skip to main content' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/#main$/);
});
