import { test, expect } from "@playwright/test";

test.describe("Landing Page E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
  });

  test("should load landing page with correct title and branding", async ({
    page,
  }) => {
    await expect(page).toHaveTitle(/SmartQueue/i);

    const brand = page.locator(".brand");
    await expect(brand).toBeVisible();
    await expect(brand).toHaveText("SmartQueue");
  });

  test("should display the sign-in form and registration CTA link", async ({
    page,
  }) => {
    const authCard = page.locator("#auth-view");
    await expect(authCard).toBeVisible();

    const heading = page.locator("h1");
    await expect(heading).toContainText("Skip the line, not the service.");

    // Check login form inputs
    const emailInput = page.locator('#login-form input[name="email"]');
    const passwordInput = page.locator('#login-form input[name="password"]');
    const submitButton = page.locator('#login-form button:not(.auth-link)');

    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitButton).toHaveText("Sign in");

    // Check "Create citizen account" link
    const createAccountBtn = page.locator(
      'a[href="/create-user"], a[href="/create-user.html"]',
    );
    await expect(createAccountBtn).toBeVisible();
    await expect(createAccountBtn).toContainText("Create citizen account");
  });

  test("should allow typing into login form fields", async ({ page }) => {
    const emailInput = page.locator('#login-form input[name="email"]');
    const passwordInput = page.locator('#login-form input[name="password"]');

    await emailInput.fill("citizen@example.com");
    await passwordInput.fill("Secret123!");

    await expect(emailInput).toHaveValue("citizen@example.com");
    await expect(passwordInput).toHaveValue("Secret123!");
  });
});
