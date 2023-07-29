import { test, expect, request } from "@playwright/test";
import { spawn } from "child_process";
import WebSocket from "ws";

const simpleWidgets = ["queue", "scoreboard", "statistics"];
const contestConfigs = [
    "config/__tests/ejudge icpc unfreeze/2023-voronezh",
    "config/__tests/ejudge ioi/regionalroi-lpk-2021-d1",
    "config/__tests/ejudge ioi virtual/mosh-2023-keldysh",
    "config/__tests/pcms icpc freeze/icpc-nef-2022-2023",
    "config/__tests/pcms icpc overrides/icpc-nef-2021-2022",
    "config/__tests/pcms ioi/innopolis-open-2022-2023-final",
    "config/__tests/testsys icpc/spbsu-2023-may"
];

const backendCooldown = 2000;
const overlayDisplayDelay = 1000;
const address = "127.0.0.1";
const startingPort = 8080;

for (const [index, contestConfig] of contestConfigs.entries()) {
    test(`config ${contestConfig}`, async ({ page }) => {
        const port = startingPort + index;
        const baseURL = `http://${address}:${port}`;
        const wsURL = `ws://${address}:${port}`;

        const childProcess = spawn("java", [
            "-jar",
            "artifacts/live-v3-dev.jar",
            `-port=${port}`,
            "-P:auth.disabled=true",
            `-P:live.configDirectory=${contestConfig}`]);

        childProcess.stdout.on("data", (data) => {
            console.log(`Child process stdout: ${data}`);
        });

        childProcess.stderr.on("data", (data) => {
            console.error(`Child process stderr: ${data}`);
        });

        childProcess.on("close", (code) => {
            console.log(`Child process exited with code ${code}`);
        });

        try {
            const adminApiContext = await request.newContext({
                baseURL: `${baseURL}/api/admin/`
            });

            await page.waitForTimeout(backendCooldown);

            let contestInfo = new WebSocket(`${wsURL}/api/overlay/contestInfo`);

            const contestOver = new Promise((resolve) => {
                if (contestInfo) {
                    contestInfo.onmessage = (event) => {
                        const message = JSON.parse(event.data.toString());
                        if (message.status === "OVER") {
                            resolve(null);
                        }
                    };
                }
            });
            await contestOver;
            contestInfo.close();

            await page.goto("/overlay");

            for (const widgetName of simpleWidgets) {
                const showWidget = await adminApiContext.post(`./${widgetName}/show`);
                expect.soft(showWidget.ok()).toBeTruthy();
            }

            await page.waitForTimeout(overlayDisplayDelay);

            const contestName = contestConfig.replace(/\//g, "_");
            await page.screenshot({ path: `tests/screenshots/${contestName}.png` });

            for (const widgetName of simpleWidgets) {
                const hideWidget = await adminApiContext.post(`./${widgetName}/hide`);
                expect.soft(hideWidget.ok()).toBeTruthy();
            }
        } finally {
            childProcess.kill();
            await page.waitForTimeout(backendCooldown);
        }
    });
}
