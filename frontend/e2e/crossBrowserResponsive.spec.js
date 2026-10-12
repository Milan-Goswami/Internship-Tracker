import { test, expect } from '@playwright/test';
import { loginOrRegister, RESPONSIVE_USER } from './authHelper';

test.describe.serial('M12.3 — Responsive Viewports & Accessibility Verification', () => {
  let page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await loginOrRegister(page, RESPONSIVE_USER);
  });

  test.afterAll(async () => {
    if (page) {
      await page.close();
    }
  });

  const viewports = [
    { name: 'desktop-1440x900', width: 1440, height: 900 },
    { name: 'laptop-1280x800', width: 1280, height: 800 },
    { name: 'tablet-768x1024', width: 768, height: 1024 },
    { name: 'mobile-390x844', width: 390, height: 844 },
    { name: 'mobile-375x667', width: 375, height: 667 },
  ];

  for (const vp of viewports) {
    test(`Viewport ${vp.name} (${vp.width}x${vp.height}): Layout, Overflow & Navigation`, async ({ browserName }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('/dashboard', { waitUntil: 'domcontentloaded' });

      // Verify Dashboard renders without horizontal overflow
      await expect(page.locator('text=Command Center')).toBeVisible();

      const overflow = await page.evaluate(() => {
        return {
          scrollWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          hasOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
        };
      });
      expect(overflow.hasOverflow).toBe(false);

      // Take screenshot for visual record
      await page.screenshot({
        path: `test-results/screenshots/dashboard-${vp.name}-${browserName}.png`,
        fullPage: false,
      });

      // Verify Applications page layout
      await page.goto('/applications', { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=Applications Workspace')).toBeVisible();

      const appsOverflow = await page.evaluate(() => {
        return document.documentElement.scrollWidth > document.documentElement.clientWidth;
      });
      expect(appsOverflow).toBe(false);

      await page.screenshot({
        path: `test-results/screenshots/applications-${vp.name}-${browserName}.png`,
        fullPage: false,
      });
    });
  }

  test('Modal & Drawer Escape Dismissal Across Viewports', async () => {
    // Open opportunities & create custom opportunity
    await page.goto('/opportunities', { waitUntil: 'domcontentloaded' });
    const addBtn = page.getByRole('button', { name: /\+ Add Custom Opportunity/i }).first();
    await addBtn.click();

    // Verify modal is visible
    const modal = page.locator('.modal-card, [role="dialog"]').first();
    await expect(modal).toBeVisible();

    // Press Escape to dismiss
    await page.keyboard.press('Escape');
    await expect(modal).toBeHidden();
  });

  test('Accessible Focus & Global Keyboard Shortcuts', async () => {
    await page.goto('/dashboard', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('text=Command Center')).toBeVisible();

    // Ensure document window has active focus across all browser engines
    await page.locator('body').click();

    // Test / shortcut to focus global search
    await page.keyboard.press('/');
    const searchInput = page.locator('.global-search-input');
    await expect(searchInput).toBeFocused();

    // Test Escape to blur search
    await page.keyboard.press('Escape');
    await expect(searchInput).not.toBeFocused();
  });
});
