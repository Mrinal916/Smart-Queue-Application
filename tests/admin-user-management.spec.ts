import { test, expect } from "@playwright/test";

test.describe("Admin User Management E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem(
        "smartqueue.auth",
        JSON.stringify({
          accessToken: "mock-admin-token",
          email: "admin@smartqueue.com",
          role: "ADMIN",
        }),
      );
    });

    await page.route("/api/v1/analytics/dashboard", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: {} }),
      });
    });

    let usersList = [
      {
        publicId: "usr-1",
        email: "citizen1@smartqueue.com",
        role: "CITIZEN",
        enabled: true,
      },
      {
        publicId: "usr-2",
        email: "officer1@smartqueue.com",
        role: "OFFICER",
        enabled: true,
      },
      {
        publicId: "usr-3",
        email: "admin@smartqueue.com",
        role: "ADMIN",
        enabled: true,
      },
    ];

    await page.route("/api/v1/users", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: usersList }),
      });
    });

    await page.route("/api/v1/users/tokens*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: { content: [] } }),
      });
    });

    await page.route("/api/v1/offices", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: [] }),
      });
    });

    // Mock update role endpoint
    await page.route("/api/v1/users/usr-1/role", async (route) => {
      const body = route.request().postDataJSON();
      const user = usersList.find((u) => u.publicId === "usr-1");
      if (user) user.role = body.role;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: user }),
      });
    });

    // Mock the reversible disable endpoint.
    await page.route("/api/v1/users/usr-1/disable", async (route) => {
      const user = usersList.find((u) => u.publicId === "usr-1");
      if (user) user.enabled = false;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: user }),
      });
    });

    await page.reload();
  });

  test("should display users directory table with email, role selector, and action buttons", async ({
    page,
  }) => {
    await page.click('button[data-tab="users"]');

    const table = page.locator("table").first();
    await expect(table).toBeVisible();
    await expect(table).toContainText("citizen1@smartqueue.com");
    await expect(table).toContainText("officer1@smartqueue.com");
    await expect(table).toContainText("admin@smartqueue.com");

    // Role selector for citizen1
    const roleSelect = page.locator('select[data-role-select="usr-1"]');
    await expect(roleSelect).toBeVisible();
    await expect(roleSelect).toHaveValue("CITIZEN");
  });

  test("should allow changing a user role from CITIZEN to OFFICER", async ({
    page,
  }) => {
    await page.click('button[data-tab="users"]');

    // Auto-accept confirmation dialog
    page.on("dialog", async (dialog) => {
      await dialog.accept();
    });

    const roleSelect = page.locator('select[data-role-select="usr-1"]');
    await roleSelect.selectOption("OFFICER");

    const updateBtn = page.locator('button[data-user-role="usr-1"]');
    await updateBtn.click();

    // Verify role selection saved
    await expect(roleSelect).toHaveValue("OFFICER");
  });

  test("should disable a user account without deleting it", async ({
    page,
  }) => {
    await page.click('button[data-tab="users"]');

    page.on("dialog", async (dialog) => {
      await dialog.accept();
    });

    const disableButton = page.locator('button[data-user-account-id="usr-1"]');
    await expect(disableButton).toHaveText("Disable user");
    await disableButton.click();

    const notice = page.locator("#notice");
    await expect(notice).toContainText("User account disabled successfully");
    await expect(page.locator("table").first()).toContainText("DISABLED");
  });
});
