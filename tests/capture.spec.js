// @ts-check
import { test, expect, request } from "@playwright/test";
import { spawn } from "child_process";

const simpleWidgets = ["scoreboard", "statistics", "queue"];
const contestConfigs = ["config/icpc-nef/2020-2021/onsite"];

const baseURL = "http://127.0.0.1:8080";

for (const contestConfig of contestConfigs) {
    test(`config ${contestConfig}`, async ({ page }) => {
        const childProcess = spawn("java", ["-jar", "artifacts/live-v3-dev.jar", "-P:auth.disabled=true", `-P:live.configDirectory=${contestConfig}`]);

        childProcess.stdout.on("data", (data) => {
            console.log(`Child process stdout: ${data}`);
        });

        childProcess.stderr.on("data", (data) => {
            console.error(`Child process stderr: ${data}`);
        });

        childProcess.on("close", (code) => {
            console.log(`Child process exited with code ${code}`);
        });

        await page.waitForTimeout(10000);
        await page.goto("/overlay");

        const adminApiContext = await request.newContext({
            baseURL: `${baseURL}/api/admin/`
        });

        for (const widgetName of simpleWidgets) {
            const showWidget = await adminApiContext.post(`./${widgetName}/show`);
            expect(showWidget.ok()).toBeTruthy();
        }

        await page.waitForTimeout(10000);

        const contestName = contestConfig.replace(/\//g, "_");
        await page.screenshot({ path: `tests/screenshots/${contestName}.png` });

        for (const widgetName of simpleWidgets) {
            const hideWidget = await adminApiContext.post(`./${widgetName}/hide`);
            expect(hideWidget.ok()).toBeTruthy();
        }

        childProcess.kill();
    });
}
