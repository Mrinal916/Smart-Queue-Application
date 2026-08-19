import { test, expect } from "@playwright/test";

test.describe("Authentication Flow E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test("should display error notice on invalid login response", async ({
    page,
  }) => {
    // Mock failed login response from API
    await page.route("/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 401,
        contentType: "application/json",
        body: JSON.stringify({
          success: false,
          error: { message: "Invalid email or password" },
        }),
      });
    });

    await page.fill('#login-form input[name="email"]', "wrong@example.com");
    await page.fill('#login-form input[name="password"]', "wrongpassword");
    await page.click("#login-form button");

    const notice = page.locator("#auth-notice");
    await expect(notice).toBeVisible();
    await expect(notice).toContainText("Invalid email or password");
  });

  test("should transition to app view on successful authentication mock", async ({
    page,
  }) => {
    // Mock successful login response
    await page.route("/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: "fake-jwt-token-12345",
            email: "citizen@smartqueue.com",
            role: "CITIZEN",
          },
        }),
      });
    });

    // Mock active tokens endpoint
    await page.route("/api/v1/tokens/active", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    await page.fill(
      '#login-form input[name="email"]',
      "citizen@smartqueue.com",
    );
    await page.fill('#login-form input[name="password"]', "Password123!");
    await page.click("#login-form button");

    // Expect auth view to hide and app view to show
    await expect(page.locator("#auth-view")).toBeHidden();
    await expect(page.locator("#app-view")).toBeVisible();

    // Check session identity display
    const identity = page.locator("#identity");
    await expect(identity).toBeVisible();
    await expect(identity).toContainText("citizen@smartqueue.com");
  });

  test("should allow user to sign out and return to login view", async ({
    page,
  }) => {
    await page.route("/api/v1/tokens/active", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    // Set authenticated state in localStorage
    await page.evaluate(() => {
      localStorage.setItem(
        "smartqueue.auth",
        JSON.stringify({
          accessToken: "mock-session-token",
          email: "user@smartqueue.com",
          role: "CITIZEN",
        }),
      );
    });

    await page.reload();

    // Session bar & sign out button should be visible
    const logoutBtn = page.locator("#logout");
    await expect(logoutBtn).toBeVisible();

    await logoutBtn.click();
    await page.waitForLoadState("domcontentloaded");

    // Should return to login view
    await expect(page.locator("#auth-view")).toBeVisible();
    await expect(page.locator("#app-view")).toBeHidden();
  });
});
