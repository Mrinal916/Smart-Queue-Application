import { test, expect } from "@playwright/test";

test.describe("Browse Offices, Departments, and Services E2E Tests", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
    // Mock authenticated citizen state
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

    // Mock active token endpoint as null
    await page.route("/api/v1/tokens/active", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: null }),
      });
    });

    // Mock offices endpoint
    await page.route("/api/v1/offices", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [
            {
              publicId: "off-1",
              name: "Central Hospital",
              category: "HOSPITAL",
              address: "123 Main St",
            },
            {
              publicId: "off-2",
              name: "Metro RTO Office",
              category: "RTO",
              address: "456 Traffic Ave",
            },
          ],
        }),
      });
    });

    // Mock departments endpoint
    await page.route("/api/v1/departments?officeId=off-1", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [
            { publicId: "dep-1", name: "Cardiology", officeId: "off-1" },
            { publicId: "dep-2", name: "General OPD", officeId: "off-1" },
          ],
        }),
      });
    });

    // Mock services endpoint
    await page.route("/api/v1/services?departmentId=dep-1", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [
            {
              publicId: "srv-1",
              name: "ECG Checkup",
              departmentId: "dep-1",
              startTime: "09:00",
              endTime: "17:00",
              averageServiceMinutes: 15,
              dailyCapacity: 20,
            },
          ],
        }),
      });
    });

    await page.route("**/api/v1/tokens/available-slots*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: ["09:00", "09:15", "09:30", "09:45", "10:00"],
        }),
      });
    });

    await page.reload();
  });

  test("should navigate to Book a Visit and cascade select through Office -> Department -> Service", async ({
    page,
  }) => {
    // Navigate to Book tab
    await page.click('button[data-tab="book"]');
    await expect(page.locator("#book-form")).toBeVisible();

    // Select Service Type (Category)
    const categorySelect = page.locator("#office-category");
    await categorySelect.selectOption("HOSPITAL");

    // Office dropdown should be enabled and contain options
    const officeSelect = page.locator("#office");
    await expect(officeSelect).toBeEnabled();
    await officeSelect.selectOption("off-1");

    // Department dropdown should be enabled after office selection
    const departmentSelect = page.locator("#department");
    await expect(departmentSelect).toBeEnabled();
    await departmentSelect.selectOption("dep-1");

    // Service dropdown should be enabled after department selection
    const serviceSelect = page.locator("#service");
    await expect(serviceSelect).toBeEnabled();
    await serviceSelect.selectOption("srv-1");

    // Slot interval hint should be visible
    const intervalText = page.locator("#slot-interval");
    await expect(intervalText).toContainText("15 minutes");
  });
});
