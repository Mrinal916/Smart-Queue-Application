import { test, expect } from "@playwright/test";

test.describe("Admin Data Management E2E Tests", () => {
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

    // Mock Admin Dashboard API
    await page.route("/api/v1/analytics/dashboard", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: { totalUsers: 10, totalOffices: 2, totalTokensToday: 15 },
        }),
      });
    });

    // Mock Offices GET & POST
    let offices = [
      {
        publicId: "off-admin-1",
        name: "General Hospital",
        code: "HOSP-01",
        category: "HOSPITAL",
        address: "1 Main St",
      },
    ];
    await page.route("/api/v1/offices", async (route) => {
      if (route.request().method() === "POST") {
        const body = route.request().postDataJSON();
        const newOffice = { publicId: `off-${Date.now()}`, ...body };
        offices.push(newOffice);
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: newOffice }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: offices }),
        });
      }
    });

    // Mock Departments GET & POST
    let departments = [
      {
        publicId: "dep-admin-1",
        name: "OPD Department",
        officeId: "off-admin-1",
      },
    ];
    await page.route("/api/v1/departments*", async (route) => {
      if (route.request().method() === "POST") {
        const body = route.request().postDataJSON();
        const newDep = { publicId: `dep-${Date.now()}`, ...body };
        departments.push(newDep);
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: newDep }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: departments }),
        });
      }
    });

    // Mock Services GET & POST
    let services = [
      {
        publicId: "srv-admin-1",
        name: "General Checkup",
        departmentId: "dep-admin-1",
        dailyCapacity: 50,
        startTime: "09:00",
        endTime: "17:00",
      },
    ];
    await page.route("/api/v1/services*", async (route) => {
      if (route.request().method() === "POST") {
        const body = route.request().postDataJSON();
        const newSrv = { publicId: `srv-${Date.now()}`, ...body };
        services.push(newSrv);
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: newSrv }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: services }),
        });
      }
    });

    // Mock Counters GET, Management & POST
    let counters = [
      {
        publicId: "cnt-admin-1",
        code: "CNT-01",
        status: "CLOSED",
        officer: null,
        services: [],
      },
    ];
    await page.route("/api/v1/counters/management*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: counters }),
      });
    });

    await page.route("/api/v1/counters", async (route) => {
      if (route.request().method() === "POST") {
        const body = route.request().postDataJSON();
        const newCnt = {
          publicId: `cnt-${Date.now()}`,
          code: body.code,
          status: "CLOSED",
          officer: null,
          services: [],
        };
        counters.push(newCnt);
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: newCnt }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ success: true, data: counters }),
        });
      }
    });

    // Mock Users endpoint
    await page.route("/api/v1/users", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: [
            {
              publicId: "usr-officer-1",
              email: "officer@smartqueue.com",
              role: "OFFICER",
              enabled: true,
            },
          ],
        }),
      });
    });

    await page.reload();
  });

  test("should display admin navigation tabs and navigate to Manage Data", async ({
    page,
  }) => {
    const nav = page.locator("#nav");
    await expect(nav).toContainText("Dashboard");
    await expect(nav).toContainText("Manage data");
    await expect(nav).toContainText("Users & tokens");
    await expect(nav).toContainText("Analytics");

    await page.click('button[data-tab="manage"]');
    await expect(page.locator("#content")).toContainText("Offices");
    await expect(page.locator("#content")).toContainText("Departments");
    await expect(page.locator("#content")).toContainText("Services");
    await expect(page.locator("#content")).toContainText(
      "Counters & assignments",
    );
  });

  test("should allow creating office, department, service, and counter", async ({
    page,
  }) => {
    await page.click('button[data-tab="manage"]');

    // Create Office
    await page.fill('#office-form input[name="code"]', "RTO-SOUTH");
    await page.fill('#office-form input[name="name"]', "South RTO Branch");
    await page.fill('#office-form input[name="address"]', "789 South Road");
    await page.click("#office-form button");

    // Department section
    const deptOfficeSelect = page.locator("#department-office");
    await expect(deptOfficeSelect).toBeVisible();
    await deptOfficeSelect.selectOption("off-admin-1");

    await page.fill('#department-form input[name="name"]', "Pediatrics");
    await page.click("#department-form button");

    // Counter creation
    const counterOfficeSelect = page.locator("#counter-office");
    await expect(counterOfficeSelect).toBeVisible();
    await counterOfficeSelect.selectOption("off-admin-1");

    await page.fill('#counter-form input[name="code"]', "COUNTER-02");
    await page.click("#counter-form button");

    // Verify created items rendered in management cards/tables
    await expect(page.locator("#content")).toContainText("South RTO Branch");
  });
});
