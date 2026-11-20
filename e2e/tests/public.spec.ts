import { test, expect } from '@playwright/test';
import { alert } from './helpers';

const pages: [string, RegExp][] = [
  ['/', /Education for All|सबके लिए/],
  ['/about', /About Gyan Ganga School/],
  ['/classes', /Classes we offer/],
  ['/facilities', /Facilities/],
  ['/gallery', /Photo gallery/],
  ['/news', /Announcements/],
  ['/contact', /Contact us/],
];

test.describe('public pages', () => {
  for (const [path, heading] of pages) {
    test(`${path} loads with its heading and no console errors`, async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', (e) => errors.push(e.message));
      page.on('console', (m) => m.type() === 'error' && errors.push(m.text()));
      const res = await page.goto(path);
      expect(res?.status()).toBe(200);
      await expect(page.getByRole('heading', { level: 1 })).toContainText(heading);
      expect(errors).toEqual([]);
    });
  }

  test('home page shows school numbers, facilities and testimonials from the database', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Our Facilities' })).toBeVisible();
    await expect(page.getByText('Library').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: 'What people are saying' })).toBeVisible();
    const students = page.locator('dl dd').first();
    await expect(students).toHaveText(/^\d+$/);
  });

  test('classes page lists Nursery to Class 8 in order', async ({ page }) => {
    await page.goto('/classes');
    const names = await page.getByRole('heading', { level: 2 }).allTextContents();
    expect(names[0]).toBe('Nursery');
    expect(names).toContain('Class 8');
    expect(names.indexOf('LKG')).toBeLessThan(names.indexOf('Class 1'));
  });

  test('about page tells the founder story in English and in Hindi', async ({ page }) => {
    await page.goto('/about');
    const section = page.locator('section[aria-labelledby=founder-heading]');
    await expect(section.getByRole('heading', { level: 2 })).toHaveText(/Never Turned a Child Away/);
    await expect(section).toContainText('Madhuri Mishra');
    await expect(section).toContainText('1970');
    await expect(section).toContainText('2022');
    await expect(section).toContainText('Dr. Jaya Mishra');
    expect(await section.locator('article p').count()).toBeGreaterThanOrEqual(6);

    await page.getByRole('button', { name: 'Switch language to Hindi' }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'hi');
    await expect(section).toContainText('माधुरी मिश्रा');
    await expect(section).toContainText('डॉ. जया मिश्रा');
    await expect(section.getByRole('heading', { level: 2 })).toContainText('विद्यालय बना दिया');
  });

  test("about page shows the principal's name, photo and message in English and Hindi", async ({ page }) => {
    await page.goto('/about');
    const photo = page.getByRole('img', { name: 'Dr. Jaya Mishra' });
    await photo.scrollIntoViewIfNeeded(); // the photo loads lazily, only once it is on screen
    await expect(photo).toBeVisible();
    await expect.poll(() => photo.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)).toBe(true);
    await expect(page.getByRole('heading', { level: 2, name: "Principal's message" })).toBeVisible();
    const message = page.locator('blockquote');
    await expect(message).toContainText('Welcome to Gyan Ganga School');
    await expect(message).toContainText('Madhuri Mishra');
    await expect(message).toContainText('Dr. Jaya Mishra');

    await page.getByRole('button', { name: 'Switch language to Hindi' }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'hi');
    await expect(page.locator('blockquote')).toContainText('ज्ञान गंगा स्कूल में आपका स्वागत है');
    await expect(page.locator('blockquote')).toContainText('डॉ. जया मिश्रा');
    await expect(page.getByRole('img', { name: 'डॉ. जया मिश्रा' })).toBeVisible();
  });

  test('facilities list matches the school: no canteen or transport, science and computer labs', async ({ page }) => {
    await page.goto('/facilities');
    const titles = await page.getByRole('heading', { level: 2 }).allTextContents();
    expect(titles).toContain('Science Laboratory');
    expect(titles).toContain('Computer Lab');
    expect(titles).not.toContain('Canteen');
    expect(titles).not.toContain('Transport');
    expect(titles).not.toContain('Laboratories');
  });

  test('unknown page shows the 404 page', async ({ page }) => {
    const res = await page.goto('/this-page-does-not-exist');
    expect(res?.status()).toBe(404);
    await expect(page.getByText('404')).toBeVisible();
  });

  test('robots.txt hides the admin and sitemap lists the pages', async ({ request }) => {
    const robots = await (await request.get('/robots.txt')).text();
    expect(robots).toContain('Disallow: /admin');
    const sitemap = await (await request.get('/sitemap.xml')).text();
    expect(sitemap).toContain('/classes');
    expect(sitemap).not.toContain('/admin');
  });
});

test.describe('language and theme', () => {
  test('switching to Hindi translates the page and is remembered', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Switch language to Hindi' }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'hi');
    await expect(page.getByRole('navigation', { name: 'Main' }).getByRole('link').first()).toHaveText('मुख्य पृष्ठ');
    await page.goto('/classes');
    await expect(page.locator('html')).toHaveAttribute('lang', 'hi');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('हमारी कक्षाएँ');
    // back to English
    await page.getByRole('button', { name: /भाषा/ }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  });

  test('dark mode toggles and survives a reload', async ({ page }) => {
    await page.goto('/');
    const html = page.locator('html');
    await page.getByRole('button', { name: 'Switch light or dark mode' }).click();
    const dark = await html.evaluate((el) => el.classList.contains('dark'));
    await page.reload();
    expect(await html.evaluate((el) => el.classList.contains('dark'))).toBe(dark);
    await page.getByRole('button', { name: 'Switch light or dark mode' }).click();
    expect(await html.evaluate((el) => el.classList.contains('dark'))).toBe(!dark);
  });
});

test.describe('admission enquiry form', () => {
  test('a valid enquiry is accepted', async ({ page }) => {
    await page.goto('/contact');
    await page.getByLabel('Parent / guardian name').fill('E2E Parent ' + Date.now());
    await page.getByLabel('Phone number').fill('9811122233');
    await page.getByLabel('Email (optional)').fill('parent@example.com');
    await page.getByLabel('Class of interest').selectOption({ label: 'Nursery' });
    await page.getByLabel('Message (optional)').fill('Please share the fee details.');
    await page.getByRole('button', { name: 'Send enquiry' }).click();
    await expect(page.getByRole('status')).toContainText('received your enquiry');
  });

  test('a bad phone number is rejected with a message and the form stays', async ({ page }) => {
    await page.goto('/contact');
    await page.getByLabel('Parent / guardian name').fill('Bad Phone');
    await page.getByLabel('Phone number').fill('abc');
    await page.getByRole('button', { name: 'Send enquiry' }).click();
    await expect(alert(page)).toBeVisible();
    await expect(page.getByLabel('Parent / guardian name')).toHaveValue('Bad Phone');
  });

  test('the hidden spam field cannot be seen or reached with the keyboard', async ({ page }) => {
    await page.goto('/contact');
    const honeypot = page.locator('#website');
    await expect(honeypot).toHaveAttribute('tabindex', '-1');
    const box = await honeypot.boundingBox();
    expect(box === null || box.x < -1000 || box.width === 0 || box.height === 0).toBe(true);
  });

  test('the map and contact details are shown', async ({ page }) => {
    await page.goto('/contact');
    await expect(page.locator('iframe[title="School location on map"]')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Address' })).toBeVisible();
  });
});

test.describe('photo gallery', () => {
  test('album opens in a lightbox that works with the keyboard', async ({ page }) => {
    await page.goto('/gallery');
    await page.locator('ul a[href^="/gallery/"]').first().click();
    await expect(page).toHaveURL(/\/gallery\/\d+$/);
    await page.locator('ul button').first().click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Close' })).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
  });

  test('category filter hides albums of other categories', async ({ page }) => {
    await page.goto('/gallery');
    const all = await page.locator('ul a[href^="/gallery/"]').count();
    expect(all).toBeGreaterThan(1);
    await page.getByRole('button', { name: 'Sports', exact: true }).click();
    const sports = await page.locator('ul a[href^="/gallery/"]').count();
    expect(sports).toBeLessThan(all);
    expect(sports).toBeGreaterThan(0);
    await expect(page.getByRole('button', { name: 'Sports', exact: true })).toHaveAttribute('aria-pressed', 'true');
  });
});

test.describe('mobile layout', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  for (const path of ['/', '/about', '/classes', '/facilities', '/gallery', '/news', '/contact']) {
    test(`${path} does not scroll sideways`, async ({ page }) => {
      await page.goto(path);
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
      expect(overflow).toBe(false);
    });
  }

  test('menu opens, navigates and closes', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('navigation', { name: 'Main' })).toBeHidden();
    await page.getByRole('button', { name: 'Open menu' }).click();
    await page.getByRole('navigation', { name: 'Mobile' }).getByRole('link', { name: 'Classes' }).click();
    await expect(page).toHaveURL(/\/classes$/);
    await expect(page.getByRole('navigation', { name: 'Mobile' })).toBeHidden();
  });
});
