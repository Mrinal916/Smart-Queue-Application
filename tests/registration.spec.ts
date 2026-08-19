import { test, expect } from "@playwright/test";

test.describe("Registration Page E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/create-user.html");
  });

  test("should load registration page with header and form elements", async ({
    page,
  }) => {
    await expect(page).toHaveTitle(/Create account | SmartQueue/i);

    const title = page.locator("#create-user-title");
    await expect(title).toBeVisible();
    await expect(title).toHaveText("Get started in a few moments.");

    const form = page.locator("#create-user-form");
    await expect(form).toBeVisible();

    const emailInput = form.locator('input[name="email"]');
    const passwordInput = form.locator('input[name="password"]');
    const confirmInput = form.locator('input[name="confirmPassword"]');
    const submitBtn = form.locator('button[type="submit"]');

    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(confirmInput).toBeVisible();
    await expect(submitBtn).toHaveText("Create account");
  });

  test("should navigate back to sign in page when clicking link", async ({
    page,
  }) => {
    const signInLink = page
      .locator('a[href="/"]', { hasText: "Sign in" })
      .first();
    await expect(signInLink).toBeVisible();

    await signInLink.click();
    await expect(page.locator("#auth-view")).toBeVisible();
  });

  test("should set custom validity on mismatched password", async ({
    page,
  }) => {
    await page.fill('input[name="email"]', "testuser@example.com");
    await page.fill('input[name="password"]', "Password123!");
    await page.fill('input[name="confirmPassword"]', "Different123!");

    const confirmInput = page.locator('input[name="confirmPassword"]');
    const validationMessage = await confirmInput.evaluate(
      (el: HTMLInputElement) => el.validationMessage,
    );
    expect(validationMessage).toContain("do not match");
  });

  test("should enforce minlength 8 password requirement in HTML validation", async ({
    page,
  }) => {
    const passwordInput = page.locator('input[name="password"]');
    await passwordInput.fill("short");

    const isValid = await passwordInput.evaluate((el: HTMLInputElement) =>
      el.checkValidity(),
    );
    expect(isValid).toBe(false);
  });

  test("should display server error notice on duplicate email registration", async ({
    page,
  }) => {
    await page.route("/api/v1/auth/register", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({
          success: false,
          error: { message: "An account with this email already exists" },
        }),
      });
    });

    await page.fill('input[name="email"]', "existing@example.com");
    await page.fill('input[name="password"]', "ValidPass123!");
    await page.fill('input[name="confirmPassword"]', "ValidPass123!");
    await page.click('button[type="submit"]');

    const message = page.locator("#form-message");
    await expect(message).toBeVisible();
    await expect(message).toContainText("email already exists");
  });

  test("should succeed registration and redirect to app home", async ({
    page,
  }) => {
    await page.route("/api/v1/auth/register", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: "token-reg-123",
            email: "newcitizen@example.com",
            role: "CITIZEN",
          },
        }),
      });
    });

    await page.fill('input[name="email"]', "newcitizen@example.com");
    await page.fill('input[name="password"]', "ValidPass123!");
    await page.fill('input[name="confirmPassword"]', "ValidPass123!");
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL("/");
  });
});
