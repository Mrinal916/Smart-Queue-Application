import { test, expect } from "@playwright/test";

test.describe("Appointment Booking E2E Tests", () => {
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

    let hasBooked = false;

    // Mock active token endpoint
    await page.route("/api/v1/tokens/active", async (route) => {
      if (hasBooked) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              publicId: "token-999",
              tokenNumber: 42,
              status: "WAITING",
              serviceName: "ECG Checkup",
              officeName: "Central Hospital",
              departmentName: "Cardiology",
              queueDate: "2026-08-10",
              appointmentTime: "10:00",
              visitorName: "John Doe",
              visitorPhone: "+919876543210",
              visitorAge: 30,
              visitorGender: "MALE",
            },
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: null }),
        });
      }
    });

    // Mock catalog APIs
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
          ],
        }),
      });
    });

    await page.route("/api/v1/departments?officeId=off-1", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [{ publicId: "dep-1", name: "Cardiology", officeId: "off-1" }],
        }),
      });
    });

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

    // Mock booking POST endpoint
    await page.route("/api/v1/tokens", async (route) => {
      hasBooked = true;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { publicId: "token-999", tokenNumber: 42, status: "WAITING" },
        }),
      });
    });

    // Mock wait-time endpoint
    await page.route("/api/v1/tokens/token-999/wait-time", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { peopleAhead: 3, estimatedMinutes: 45 },
        }),
      });
    });

    await page.reload();
  });

  test("should fill booking form, submit, and display booked appointment on My Queue", async ({
    page,
  }) => {
    await page.click('button[data-tab="book"]');
    await expect(page.locator("#book-form")).toBeVisible();

    await page.fill('input[name="visitorName"]', "John Doe");
    await page.fill('input[name="visitorPhone"]', "+919876543210");
    await page.fill('input[name="visitorAge"]', "30");
    await page.selectOption('select[name="visitorGender"]', "MALE");

    // Fill appointment date to future date first so time slots are available
    const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
    const dateInput = page.locator('input[name="appointmentDate"]');
    await dateInput.fill(tomorrow);
    await dateInput.dispatchEvent("change");

    await page.selectOption("#office-category", "HOSPITAL");

    const officeSelect = page.locator("#office");
    await expect(officeSelect).toBeEnabled();
    await officeSelect.selectOption("off-1");

    const departmentSelect = page.locator("#department");
    await expect(departmentSelect).toBeEnabled({ timeout: 10000 });
    await departmentSelect.selectOption("dep-1");

    const serviceSelect = page.locator("#service");
    await expect(serviceSelect).toBeEnabled({ timeout: 10000 });
    await serviceSelect.selectOption("srv-1");

    const timeSlotSelect = page.locator("#appointment-time");
    await expect(timeSlotSelect).toBeEnabled({ timeout: 10000 });
    await timeSlotSelect.selectOption({ index: 1 });

    await page.click("#book-form button");

    // Should show success notice
    const notice = page.locator("#notice");
    await expect(notice).toBeVisible({ timeout: 10000 });
    await expect(notice).toContainText("Appointment booked successfully");

    // Verify active appointment is rendered on My Queue view
    const apptCard = page.locator("#app-view");
    await expect(apptCard).toContainText("ECG Checkup");
    await expect(apptCard).toContainText("Central Hospital");
    await expect(apptCard).toContainText("John Doe");
  });
});
