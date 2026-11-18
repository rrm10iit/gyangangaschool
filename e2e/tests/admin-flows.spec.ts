import { expect, test, type Page } from '@playwright/test';
import { alert } from './helpers';

test.describe.configure({ mode: 'serial' });
test.beforeEach(({ page }) => {
  page.on('dialog', (d) => d.accept()); // "Delete this?" confirmations
});

const stamp = Date.now().toString().slice(-7);
const studentName = `E2E Student ${stamp}`;
const admissionNo = `E2E-${stamp}`;

async function searchRow(page: Page, text: string) {
  await page.getByRole('textbox', { name: 'Search' }).fill(text);
  const row = page.locator('tbody tr', { hasText: text });
  await expect(row).toHaveCount(1);
  return row;
}

test.describe('students', () => {
  test('add a student, see the clear error for a duplicate admission number, search and edit', async ({ page }) => {
    await page.goto('/admin/students');
    await page.getByRole('button', { name: 'Add student' }).click();
    await page.getByLabel('Admission number').fill(admissionNo);
    await page.getByLabel('Student name').fill(studentName);
    await page.getByLabel('Class *').selectOption({ label: 'Class 2' });
    await page.getByLabel('Guardian name').fill('E2E Guardian');
    await page.getByLabel('Guardian phone').fill('9000000001');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Added student.')).toBeVisible();

    // same admission number again
    await page.getByRole('button', { name: 'Add student' }).click();
    await page.getByLabel('Admission number').fill(admissionNo);
    await page.getByLabel('Student name').fill('Duplicate');
    await page.getByLabel('Class *').selectOption({ label: 'Class 2' });
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(alert(page)).toContainText('already in use');
    await page.getByRole('button', { name: 'Cancel' }).click();

    // required fields are reported next to the field
    await page.getByRole('button', { name: 'Add student' }).click();
    await page.getByLabel('Admission number').fill('X-' + stamp);
    await page.getByLabel('Class *').selectOption({ label: 'Class 2' });
    await page.getByLabel('Student name').fill(' ');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(alert(page)).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();

    const row = await searchRow(page, studentName);
    await row.getByRole('button', { name: 'Edit' }).click();
    await page.getByLabel('Section').fill('B');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Saved student.')).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'Class 2 - B' })).toHaveCount(1);
  });

  test('filter by class narrows the list', async ({ page }) => {
    await page.goto('/admin/students');
    await expect(page.locator('tbody tr').first()).toBeVisible();
    await page.getByLabel('Class', { exact: true }).selectOption({ label: 'Class 2' });
    await expect(page.getByText(studentName)).toBeVisible();
    await page.getByLabel('Class', { exact: true }).selectOption({ label: 'Nursery' });
    await expect(page.getByText(studentName)).toBeHidden();
  });

  test('upload and remove a document', async ({ page }) => {
    await page.goto('/admin/students');
    const row = await searchRow(page, studentName);
    await row.getByRole('button', { name: new RegExp(`Documents for ${studentName}`) }).click();
    await page.getByLabel('Document name').fill('Birth certificate');
    await page.getByLabel(/File \(PDF/).setInputFiles({
      name: 'birth.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4\n%%EOF'),
    });
    await page.getByRole('button', { name: 'Upload' }).click();
    const link = page.getByRole('link', { name: 'Birth certificate' });
    await expect(link).toBeVisible();
    const href = await link.getAttribute('href');
    const res = await page.request.get(href!);
    expect(res.status()).toBe(200);
    expect(res.headers()['content-type']).toBe('application/pdf');

    // a script renamed to .pdf is refused
    await page.getByLabel('Document name').fill('Fake');
    await page.getByLabel(/File \(PDF/).setInputFiles({ name: 'fake.pdf', mimeType: 'application/pdf', buffer: Buffer.from('<script>1</script>') });
    await page.getByRole('button', { name: 'Upload' }).click();
    await expect(alert(page)).toContainText('Only PDF, JPG or PNG');

    await page.getByRole('button', { name: 'Delete Birth certificate' }).click();
    await expect(link).toBeHidden();
  });
});

test.describe('fees', () => {
  test('record a payment, print the receipt, then cancel it and delete the student', async ({ page }) => {
    await page.goto('/admin/finance/payments');
    await page.getByRole('button', { name: 'Record payment' }).click();
    await page.getByPlaceholder('Type a name or admission number').fill(studentName);
    await page.getByRole('button', { name: new RegExp(studentName) }).click();
    await expect(page.getByText('Yearly fee')).toBeVisible();
    await page.getByLabel('Amount received (Rs.) *').fill('1234');
    await page.getByLabel('Paid for').fill('Tuition e2e');
    await page.getByRole('button', { name: 'Save and create receipt' }).click();
    await expect(page.getByText('has been created')).toBeVisible();

    const href = await page.getByRole('link', { name: /printable receipt/ }).getAttribute('href');
    const pdf = await page.request.get(href!);
    expect(pdf.status()).toBe(200);
    expect(pdf.headers()['content-type']).toBe('application/pdf');
    expect((await pdf.body()).subarray(0, 5).toString()).toBe('%PDF-');
    await page.getByText('Close', { exact: true }).click();

    // the student cannot be deleted while a receipt exists
    await page.goto('/admin/students');
    let row = await searchRow(page, studentName);
    await row.getByRole('button', { name: 'Delete' }).click();
    await expect(alert(page)).toContainText('still in use');

    await page.goto('/admin/finance/payments');
    row = await searchRow(page, studentName);
    await expect(row).toContainText('₹1,234');
    await row.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText(/Deleted receipt/)).toBeVisible();

    await page.goto('/admin/students');
    row = await searchRow(page, studentName);
    await row.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText(/Deleted student/)).toBeVisible();
  });

  test('add a fee head, an expense and a salary record', async ({ page }) => {
    await page.goto('/admin/finance/expenses');
    await page.getByRole('button', { name: 'Add expense' }).click();
    await page.getByLabel('Category *').selectOption('Stationery');
    await page.getByLabel('Description *').fill('E2E chalk ' + stamp);
    await page.getByLabel('Amount (Rs.) *').fill('250');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Added expense.')).toBeVisible();
    const row = await searchRow(page, 'E2E chalk ' + stamp);
    await expect(row).toContainText('₹250');
    await row.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText(/Deleted expense/)).toBeVisible();
  });
});

test.describe('website content', () => {
  test('an edit made in the admin panel shows on the public site straight away', async ({ page }) => {
    const original = 'Education for All, Excellence for Each';
    const edited = 'E2E edit ' + stamp;
    await page.goto('/admin/content/settings');
    const box = page.getByLabel('Hero title (English)');
    try {
      await box.fill(edited);
      await page.getByRole('button', { name: /Save changes/ }).click();
      await expect(page.getByText(/Saved\./)).toBeVisible();
      await page.goto('/');
      await expect(page.getByRole('heading', { level: 1 })).toHaveText(edited);
    } finally {
      await page.goto('/admin/content/settings');
      await page.getByLabel('Hero title (English)').fill(original);
      await page.getByRole('button', { name: /Save changes/ }).click();
      await expect(page.getByText(/Saved\./)).toBeVisible();
    }
  });

  test('an announcement can be added, pinned and removed', async ({ page }) => {
    const title = 'E2E notice ' + stamp;
    await page.goto('/admin/content/announcements');
    await page.getByRole('button', { name: 'Add announcement' }).click();
    await page.getByLabel('Title (English) *').fill(title);
    await page.getByLabel('Details (English)').fill('Details here');
    await page.getByLabel('Pin to the top as important').check();
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Added announcement.')).toBeVisible();

    await page.goto('/news');
    await expect(page.getByRole('heading', { name: title })).toBeVisible();

    await page.goto('/admin/content/announcements');
    const row = await searchRow(page, title);
    await row.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText(/Deleted announcement/)).toBeVisible();
  });

  test('upload photos into an album and see them on the public gallery', async ({ page }) => {
    const album = 'E2E album ' + stamp;
    await page.goto('/admin/content/gallery');
    await page.getByRole('button', { name: 'New album' }).click();
    await page.getByLabel('Album title (English) *').fill(album);
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Added album.')).toBeVisible();

    const row = await searchRow(page, album);
    await row.getByRole('button', { name: new RegExp(`Manage photos in ${album}`) }).click();
    const png = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAfbLI3wAAAABJRU5ErkJggg==', 'base64');
    await page.getByLabel(/Choose photos/).setInputFiles([
      { name: 'a.png', mimeType: 'image/png', buffer: png },
      { name: 'b.png', mimeType: 'image/png', buffer: png },
    ]);
    await page.getByRole('button', { name: 'Upload' }).click();
    await expect(page.getByRole('dialog').locator('img')).toHaveCount(2);
    const src = await page.getByRole('dialog').locator('img').first().getAttribute('src');
    expect((await page.request.get(src!)).status()).toBe(200);
    await page.keyboard.press('Escape');

    await page.goto('/gallery');
    await expect(page.getByRole('heading', { name: album })).toBeVisible();
    await expect(page.getByText('2 photos').first()).toBeVisible();

    await page.goto('/admin/content/gallery');
    const again = await searchRow(page, album);
    await again.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText(/Deleted album/)).toBeVisible();
  });

  test('an enquiry sent from the public site appears here and its status can be changed', async ({ page, browser }) => {
    const name = 'E2E Parent ' + stamp;
    const visitor = await browser.newPage();
    await visitor.goto('/contact');
    await visitor.getByLabel('Parent / guardian name').fill(name);
    await visitor.getByLabel('Phone number').fill('9811100000');
    await visitor.getByRole('button', { name: 'Send enquiry' }).click();
    await expect(visitor.getByRole('status')).toContainText('received your enquiry');
    await visitor.close();

    await page.goto('/admin/content/enquiries');
    const row = await searchRow(page, name);
    await expect(row).toContainText('NEW');
    await row.getByRole('button', { name: 'Edit' }).click();
    await page.getByLabel('Status *').selectOption('CONTACTED');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Saved enquiry.')).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: name })).toContainText('CONTACTED');
  });
});

test.describe('activity log and account', () => {
  test('changes are in the activity log with who made them', async ({ page }) => {
    await page.goto('/admin/activity');
    await expect(page.locator('tbody tr', { hasText: 'E2E' }).first()).toBeVisible();
    await page.getByRole('textbox', { name: 'Search' }).fill('Added');
    await expect(page.locator('tbody tr').first()).toContainText('admin@');
  });

  test('changing the password needs the current one and matching confirmation', async ({ page }) => {
    await page.goto('/admin/account');
    await page.getByLabel('Current password').fill('wrong');
    await page.getByLabel('New password', { exact: true }).fill('NewPassword-123');
    await page.getByLabel('Confirm new password').fill('Different-123');
    await page.getByRole('button', { name: 'Change password' }).click();
    await expect(alert(page)).toContainText('do not match');

    await page.getByLabel('Confirm new password').fill('NewPassword-123');
    await page.getByRole('button', { name: 'Change password' }).click();
    await expect(alert(page)).toContainText('Current password is incorrect');
  });

  test('signing out ends the session', async ({ page }) => {
    await page.goto('/admin');
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page).toHaveURL(/\/admin\/login$/);
    await page.goto('/admin/students');
    await expect(page).toHaveURL(/\/admin\/login$/);
  });
});
