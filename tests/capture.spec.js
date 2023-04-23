// @ts-check
import { test, expect } from "@playwright/test";

let adminApiContext;
let page;


test.beforeAll(async ({ playwright, baseURL, browser }) => {
    page = await browser.newPage();
    await page.waitForTimeout(1000);
    await page.goto("/overlay");
    adminApiContext = await playwright.request.newContext({
        baseURL: `${baseURL}/api/admin/`
    });
});

test.afterAll(async () => {
    await page.close();
});

// Uniform screenshots for all platforms
// https://github.com/microsoft/playwright/issues/7575
// eslint-disable-next-line no-empty-pattern
test.beforeEach(async ({ }, testInfo) => {
    testInfo.snapshotPath = (name) =>
        `${testInfo.file}-snapshots/${name}`;
});

const simpleWidgets = ["scoreboard", "statistics"];

for (const widgetName of simpleWidgets) {
    test(`test ${widgetName}`, async () => {
        const showWidget = await adminApiContext.post(`./${widgetName}/show`);
        expect(showWidget.ok()).toBeTruthy();

        await expect(page).toHaveScreenshot();

        const hideWidget = await adminApiContext.post(`./${widgetName}/hide`);
        expect(hideWidget.ok()).toBeTruthy();
    });
}
