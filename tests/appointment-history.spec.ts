import { test, expect } from "@playwright/test";

test.describe("Appointment History and Wait-Time Display E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
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
        body: JSON.stringify({
          success: true,
          data: {
            publicId: "token-hist-1",
            tokenNumber: 5,
            status: "WAITING",
            serviceName: "Cardiology Visit",
            officeName: "City Hospital",
            departmentName: "Cardiology",
            queueDate: "2026-08-10",
            appointmentTime: "09:30",
          },
        }),
      });
    });

    await page.route("/api/v1/tokens/token-hist-1/wait-time", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { peopleAhead: 4, estimatedMinutes: 60 },
        }),
      });
    });

    await page.route("/api/v1/offices", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: [] }),
      });
    });

    await page.route(
      "/api/v1/tokens/history?page=0&size=100",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              content: [
                {
                  publicId: "token-hist-1",
                  tokenNumber: 5,
                  queueDate: "2026-08-10",
                  appointmentTime: "09:30",
                  status: "WAITING",
                  serviceId: "srv-1",
                },
                {
                  publicId: "token-hist-2",
                  tokenNumber: 1,
                  queueDate: "2026-08-01",
                  appointmentTime: "10:00",
                  status: "COMPLETED",
                  serviceId: "srv-1",
                },
              ],
            },
          }),
        });
      },
    );

    await page.reload();
  });

  test("should display wait time estimates (people ahead and approx minutes) on active token", async ({
    page,
  }) => {
    const tokenInfo = page.locator("#token-info");
    await expect(tokenInfo).toBeVisible();
    await expect(tokenInfo).toContainText("4 people ahead");
    await expect(tokenInfo).toContainText("1 hr");
  });

  test("should view appointment history table with past tokens", async ({
    page,
  }) => {
    await page.click('button[data-tab="history"]');

    const historySection = page.locator("#content");
    await expect(historySection).toContainText("Appointment history");

    const table = page.locator("table");
    await expect(table).toBeVisible();
    await expect(table).toContainText("WAITING");
    await expect(table).toContainText("COMPLETED");
  });
});
