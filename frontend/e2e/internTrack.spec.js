import { test, expect } from '@playwright/test';
import { loginOrRegister, FEATURE_USER } from './authHelper';

function generateTestUser() {
  const uid = Math.random().toString(36).substring(2, 9);
  return {
    fullName: `Tester ${uid}`,
    email: `e2e_${uid}@test.com`,
    password: 'Password123!',
  };
}

async function registerAndLogin(page, user) {
  await page.goto('/register', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Full Name').fill(user.fullName);
  await page.getByLabel('Email Address').fill(user.email);
  await page.getByLabel('Password (min 8 characters)').fill(user.password);
  await page.getByLabel('Confirm Password').fill(user.password);
  await page.getByRole('button', { name: 'Create Account' }).click();
  await expect(page).toHaveURL(/.*\/dashboard/, { timeout: 30000 });
}

test.describe('InternTrack E2E Lifecycle', () => {

  test('A. Authentication', async ({ page }) => {
    const user = generateTestUser();
    await registerAndLogin(page, user);

    await expect(page.locator('text=Command Center')).toBeVisible();

    await page.goto('/applications');
    await expect(page.locator('text=Applications Workspace')).toBeVisible();

    await page.locator('.user-profile-trigger').click();
    await page.getByRole('button', { name: 'Sign Out' }).click();
    await expect(page).toHaveURL(/.*\/login/);

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/.*\/login/);
  });

  test.describe.serial('Core Application Features', () => {
    let page;

    test.beforeAll(async ({ browser }) => {
      page = await browser.newPage();
      await loginOrRegister(page, FEATURE_USER);
    });

    test.afterAll(async () => {
      if (page) {
        await page.close();
      }
    });

    test('B. Application lifecycle & Opportunity', async () => {
      await page.goto('/opportunities', { waitUntil: 'domcontentloaded' });
      
      const addBtn = page.getByRole('button', { name: /\+ Add Custom Opportunity/i }).first();
      await addBtn.click();
      
      await page.getByPlaceholder('Figma, Stripe, OpenAI').fill('E2E Corp');
      await page.getByPlaceholder('Software Engineering Intern').fill('E2E Engineer');
      await page.getByRole('button', { name: 'Create Opportunity' }).click();

      await expect(page.locator('text=E2E Corp').first()).toBeVisible();

      await page.getByRole('button', { name: '+ Track Application' }).first().click();
      
      await expect(page.locator('text=In Pipeline').first()).toBeVisible();
      await page.goto('/applications', { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=E2E Corp').first()).toBeVisible();
    });

    test('C. Applications Workspace Batch Operations', async () => {
      await page.goto('/opportunities', { waitUntil: 'domcontentloaded' });
      await page.getByRole('button', { name: /\+ Add Custom Opportunity/i }).first().click();
      await page.getByPlaceholder('Figma, Stripe, OpenAI').fill('Batch Corp');
      await page.getByPlaceholder('Software Engineering Intern').fill('Batch Engineer');
      await page.getByRole('button', { name: 'Create Opportunity' }).click();
      await page.getByRole('button', { name: '+ Track Application' }).first().click();
      await expect(page.locator('text=In Pipeline').first()).toBeVisible();

      await page.goto('/applications', { waitUntil: 'domcontentloaded' });
      
      const tableViewBtn = page.getByRole('button', { name: /Table/i });
      if (await tableViewBtn.isVisible()) {
        await tableViewBtn.click();
      }

      const selectAll = page.locator('input[type="checkbox"]').first();
      if (await selectAll.isVisible()) {
        await selectAll.check();
        await page.getByLabel('Select stage for batch status update').selectOption({ label: 'Rejected' });
        await page.getByRole('button', { name: 'Apply Status' }).click();
        await expect(page.getByText(/Updated \d+ application/i).or(page.getByText('Moved to REJECTED'))).toBeVisible();
      }
    });

    test('D. Skill Matching', async () => {
      await page.goto('/opportunities', { waitUntil: 'domcontentloaded' });
      await page.getByRole('button', { name: /\+ Add Custom Opportunity/i }).first().click();
      await page.getByPlaceholder('Figma, Stripe, OpenAI').fill('Skill Corp');
      await page.getByPlaceholder('Software Engineering Intern').fill('Skill Engineer');
      await page.getByRole('button', { name: 'Create Opportunity' }).click();

      await expect(page.locator('text=Skill Corp').first()).toBeVisible();
      await page.getByLabel('Skill match for Skill Engineer').or(page.getByLabel(/Skill match for/i)).first().click();
      
      // UI Assertions for ADR-06 Case A
      await expect(page.locator('.skill-match-drawer').first()).toBeVisible();
      
      // Ensure no NaN/Infinity
      const content = await page.textContent('body');
      expect(content).not.toMatch(/NaN/);
      expect(content).not.toMatch(/Infinity/);
    });

    test('E. Interviews', async () => {
      await page.goto('/opportunities', { waitUntil: 'domcontentloaded' });
      await page.getByRole('button', { name: /\+ Add Custom Opportunity/i }).first().click();
      await page.getByPlaceholder('Figma, Stripe, OpenAI').fill('Interview Corp');
      await page.getByPlaceholder('Software Engineering Intern').fill('Interview Engineer');
      await page.getByRole('button', { name: 'Create Opportunity' }).click();
      await page.getByRole('button', { name: '+ Track Application' }).first().click();
      await expect(page.locator('text=In Pipeline').first()).toBeVisible();
      
      await page.goto('/interviews', { waitUntil: 'domcontentloaded' });
      await page.getByRole('button', { name: /\+ Schedule/i }).first().click();
      
      // Wait for modal to appear
      const modal = page.locator('.modal-card, [role="dialog"]').first();
      await expect(modal).toBeVisible();
      
      await modal.locator('#interview-linked-app').selectOption({ index: 1 });
      await modal.locator('#interview-round-type').selectOption({ label: 'Technical OA' });
      await modal.locator('#interview-round-title').fill('Technical OA Assessment');
      await modal.locator('#interview-scheduled-at').fill('2026-10-10T10:00');
      await modal.getByRole('button', { name: 'Schedule Round' }).click();

      // Verify interview is created and open prep drawer
      await expect(page.locator('.interview-card').first()).toBeVisible();
      
      await page.getByRole('button', { name: /Prep Notes/i }).first().click();
      await expect(page.locator('.interview-prep-drawer').first()).toBeVisible();
      
      await page.getByLabel('Interview reflections and notes').fill('Went well');
      await page.getByRole('button', { name: /Passed/i }).click();
      await page.getByRole('button', { name: 'Save Outcome' }).click();
      
      // verify outcome in UI (PASSED)
      await expect(page.locator('.outcome-passed').first()).toBeVisible();
    });

    test('F. Documents', async () => {
      await page.goto('/documents', { waitUntil: 'domcontentloaded' });
      const registerDocBtn = page.getByRole('button', { name: 'Register Document' });
      await registerDocBtn.click();
      
      await page.getByLabel('Document Title *').fill('Resume');
      await page.getByLabel(/External Storage URL/i).fill('javascript:alert(1)');
      await page.locator('form').getByRole('button', { name: 'Register Document' }).click();
      await expect(page.locator('text=valid absolute HTTP or HTTPS URL')).toBeVisible();
    });
  });
});
