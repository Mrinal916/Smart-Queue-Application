import { test, expect } from "@playwright/test";

test.describe("Appointment Cancellation E2E Tests", () => {
  test("should allow citizen to cancel a waiting appointment from My Queue tab", async ({
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

    let isCancelled = false;

    // Mock active token endpoint
    await page.route("/api/v1/tokens/active", async (route) => {
      if (isCancelled) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: null }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              publicId: "token-active-1",
              tokenNumber: 15,
              status: "WAITING",
              serviceName: "General Consultation",
              officeName: "Central Hospital",
              departmentName: "General OPD",
              queueDate: "2026-08-10",
              appointmentTime: "11:00",
            },
          }),
        });
      }
    });

    // Mock wait-time endpoint
    await page.route(
      "/api/v1/tokens/token-active-1/wait-time",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: { peopleAhead: 1, estimatedMinutes: 15 },
          }),
        });
      },
    );

    // Mock cancel endpoint
    await page.route("/api/v1/tokens/token-active-1/cancel", async (route) => {
      isCancelled = true;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    await page.reload();

    // Verify appointment card is shown with Cancel button
    const cancelBtn = page.locator("#cancel");
    await expect(cancelBtn).toBeVisible();
    await expect(cancelBtn).toContainText("Cancel appointment");

    await cancelBtn.click();

    // Should refresh home to show no active appointment state
    await expect(page.locator("#content")).toContainText(
      "No active appointment",
    );
  });
});
