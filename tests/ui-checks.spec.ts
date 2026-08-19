import { test, expect } from "@playwright/test";

test.describe("Basic UI, Console Errors & Responsive Layout E2E Tests", () => {
  test("should load landing page without any JavaScript console or uncaught runtime errors", async ({
    page,
  }) => {
    const consoleErrors: string[] = [];
    const pageErrors: Error[] = [];

    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    page.on("pageerror", (err) => {
      pageErrors.push(err);
    });

    await page.goto("/");
    await page.waitForLoadState("domcontentloaded");

    expect(consoleErrors).toHaveLength(0);
    expect(pageErrors).toHaveLength(0);
  });

  test("should load registration page without JS console errors", async ({
    page,
  }) => {
    const consoleErrors: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto("/create-user.html");
    await page.waitForLoadState("domcontentloaded");

    expect(consoleErrors).toHaveLength(0);
  });

  test("should render properly and remain usable on mobile viewports (375x667)", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto("/");

    const brand = page.locator(".brand");
    await expect(brand).toBeVisible();

    const authForm = page.locator("#login-form");
    await expect(authForm).toBeVisible();

    const submitBtn = authForm.locator('button:not(.auth-link)');
    await expect(submitBtn).toBeVisible();

    const createAccountBtn = page.locator('a[href="/create-user"]');
    await expect(createAccountBtn).toBeVisible();
  });
});
