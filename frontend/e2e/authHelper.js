import { expect } from '@playwright/test';

export const RESPONSIVE_USER = {
  fullName: 'Responsive Suite Runner',
  email: 'responsive_runner@interntrack.internal',
  password: 'Password123!',
};

export const FEATURE_USER = {
  fullName: 'Feature Suite Runner',
  email: 'feature_runner@interntrack.internal',
  password: 'Password123!',
};

export async function loginOrRegister(page, user) {
  // Try logging in first (in case user already exists in DB)
  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Email Address').fill(user.email);
  await page.getByLabel('Password').fill(user.password);
  await page.getByRole('button', { name: 'Sign In' }).click();

  const loggedIn = await page.waitForURL(/.*\/dashboard/, { timeout: 12000 }).then(() => true).catch(() => false);
  if (loggedIn) {
    return;
  }

  // If user does not exist yet, register once
  await page.goto('/register', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Full Name').fill(user.fullName);
  await page.getByLabel('Email Address').fill(user.email);
  await page.getByLabel('Password (min 8 characters)').fill(user.password);
  await page.getByLabel('Confirm Password').fill(user.password);
  await page.getByRole('button', { name: 'Create Account' }).click();
  await expect(page).toHaveURL(/.*\/dashboard/, { timeout: 30000 });
}
