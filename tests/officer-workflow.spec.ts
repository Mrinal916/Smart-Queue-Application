import { test, expect } from "@playwright/test";

test.describe("Officer Desk and Token Operations E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    // Mock assigned officer counters with wildcard URL pattern
    await page.route("**/api/v1/officer/counters", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [
            {
              counterId: "cnt-off-1",
              counterCode: "COUNTER-1",
              officeCategory: "RTO",
              officeName: "Metro RTO",
              services: [
                { serviceId: "srv-off-1", serviceName: "Licensing Renewal" },
              ],
            },
          ],
        }),
      });
    });

    // Mock counter status
    await page.route(
      "**/api/v1/officer/counters/cnt-off-1/status",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: { code: "COUNTER-1", status: "OPEN" },
          }),
        });
      },
    );

    // Mock counter dashboard
    await page.route(
      "**/api/v1/officer/counters/cnt-off-1/dashboard*",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              counterCode: "COUNTER-1",
              counterStatus: "OPEN",
              serviceName: "Licensing Renewal",
              queueDate: "2026-08-20",
              completedCount: 5,
              cancelledCount: 1,
              averageWaitMinutes: 12,
              arrivedCount: 3,
              waitingCount: 2,
              currentToken: {
                publicId: "token-101",
                tokenNumber: 10,
                status: "CALLED",
                visitorName: "Bob",
              },
              tokens: [
                {
                  publicId: "token-102",
                  tokenNumber: 11,
                  status: "WAITING",
                  serviceId: "srv-off-1",
                  visitorName: "Alice",
                },
              ],
            },
          }),
        });
      },
    );

    // Mock call next token endpoint
    await page.route("**/api/v1/tokens/next", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { publicId: "token-102", tokenNumber: 11, status: "CALLED" },
        }),
      });
    });

    // Mock token action mutations (complete, skip, no-show)
    await page.route("**/api/v1/tokens/token-101/*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem(
        "smartqueue.auth",
        JSON.stringify({
          accessToken: "mock-officer-token",
          email: "officer@smartqueue.com",
          role: "OFFICER",
        }),
      );
    });
    await page.reload();
  });

  test("should display officer counter desk and load counter queue details", async ({
    page,
  }) => {
    const nav = page.locator("#nav");
    await expect(nav).toContainText("Counter desk");
    await expect(nav).toContainText("Token operations");

    const officerFilters = page.locator("#officer-filters");
    await expect(officerFilters).toBeVisible({ timeout: 10000 });

    const dashboard = page.locator("#officer-dashboard");
    await expect(dashboard).toBeVisible({ timeout: 10000 });
    await expect(page.locator(".officer-overview")).toContainText("COUNTER-1");
    await expect(dashboard).toContainText("2 waiting");
    await expect(dashboard).toContainText("#10");
  });

  test("should call next token from token operations tab", async ({ page }) => {
    await page.click('button[data-tab="operations"]');

    const nextForm = page.locator("#next-form");
    await expect(nextForm).toBeVisible({ timeout: 10000 });

    await page.click("#next-form button");

    const activeOp = page.locator("#active-operation");
    await expect(activeOp).toBeVisible({ timeout: 10000 });
    await expect(activeOp).toContainText("Now serving token #11");

    const notice = page.locator("#notice");
    await expect(notice).toContainText("Next token called");
  });

  test("calls only one token for repeated Call next submissions", async ({
    page,
  }) => {
    let callCount = 0;
    await page.unroute("**/api/v1/tokens/next");
    await page.route("**/api/v1/tokens/next", async (route) => {
      callCount += 1;
      await new Promise((resolve) => setTimeout(resolve, 200));
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { publicId: "token-102", tokenNumber: 11, status: "CALLED" },
        }),
      });
    });

    await page.click('button[data-tab="operations"]');
    const nextForm = page.locator("#next-form");
    await expect(nextForm).toBeVisible({ timeout: 10000 });
    await nextForm.evaluate((form) => {
      form.requestSubmit();
      form.requestSubmit();
    });

    await expect(page.locator("#active-operation")).toContainText(
      "Now serving token #11",
    );
    expect(callCount).toBe(1);
  });
});
