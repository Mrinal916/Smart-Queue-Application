import { test, expect } from "@playwright/test";

test.describe("Role Security & Permission Access E2E Tests", () => {
  test("should restrict CITIZEN role from viewing admin or officer navigation tabs", async ({
    page,
  }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem(
        "smartqueue.auth",
        JSON.stringify({
          accessToken: "mock-citizen-token",
          email: "citizen@example.com",
          role: "CITIZEN",
        }),
      );
    });

    await page.route("/api/v1/tokens/active", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    await page.reload();

    const nav = page.locator("#nav");
    await expect(nav).toBeVisible();
    await expect(nav).toContainText("My queue");
    await expect(nav).toContainText("Book a token");
    await expect(nav).toContainText("History");

    // Admin & Officer tabs should NOT be visible to Citizen
    await expect(nav).not.toContainText("Dashboard");
    await expect(nav).not.toContainText("Users & tokens");
    await expect(nav).not.toContainText("Manage data");
    await expect(nav).not.toContainText("Analytics");
    await expect(nav).not.toContainText("Counter desk");
    await expect(nav).not.toContainText("Token operations");
  });

  test("should clear session and redirect to login when API returns 401 unauthorized", async ({
    page,
  }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem(
        "smartqueue.auth",
        JSON.stringify({
          accessToken: "expired-token",
          email: "citizen@example.com",
          role: "CITIZEN",
        }),
      );
    });

    // Return 401 on tokens/active request
    await page.route("/api/v1/tokens/active", async (route) => {
      await route.fulfill({
        status: 401,
        contentType: "application/json",
        body: JSON.stringify({
          success: false,
          error: { message: "Token expired" },
        }),
      });
    });

    await page.reload();

    // Should return to login view after session clearance
    await expect(page.locator("#auth-view")).toBeVisible({ timeout: 10000 });
    await expect(page.locator("#app-view")).toBeHidden();
  });
});
