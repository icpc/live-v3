import { test, expect, request } from "@playwright/test";
import { spawn } from "child_process";
import WebSocket from "ws";

const simpleWidgets = ["queue", "scoreboard", "statistics"];
const contestConfigs = [
    "config/__tests/ejudge_icpc_unfreeze/2023-voronezh",
//    "config/__tests/ejudge_ioi/regionalroi-lpk-2021-d1",
    "config/__tests/ejudge_ioi_virtual/mosh-2023-keldysh",
    "config/__tests/pcms_icpc_freeze/icpc-nef-2022-2023",
    "config/__tests/pcms_icpc_overrides/icpc-nef-2021-2022",
    "config/__tests/pcms_ioi/innopolis-open-2022-2023-final",
    "config/__tests/testsys_icpc/spbsu-2023-may"
];

const backendStartCooldown = 3000;
const backendFinishCooldown = 1000;
const overlayDisplayDelay = 1000;
const address = "127.0.0.1";
const startingPort = 8090;

for (const [index, contestConfig] of contestConfigs.entries()) {
    test(`config ${contestConfig}`, async ({ page }, testInfo) => {
        console.log(`Starting contest ${contestConfig}`)
        const port = startingPort + index;
        const baseURL = `http://${address}:${port}`;
        const wsURL = `ws://${address}:${port}`;

        const childProcess = spawn("java", [
            "-jar",
            "artifacts/live-v3-dev.jar",
            "-p",`${port}`,
            "--no-auth",
            "-c", `${contestConfig}`
            ]);

        childProcess.stdout.on("data", (data) => {
            console.log(`${data}`);
        });

        childProcess.stderr.on("data", (data) => {
            console.error(`${data}`);
        });

        childProcess.on("close", (code) => {
            console.log(`Child process exited with code ${code}`);
        });

        process.on('exit', function () {
            childProcess.kill();
        });

        process.on('SIGINT', function () {
            childProcess.kill();
            process.exit();
        });

        const adminApiContext = await request.newContext({
            baseURL: `${baseURL}/api/admin/`
        });

        await page.waitForTimeout(backendStartCooldown);

        let contestInfo = new WebSocket(`${wsURL}/api/overlay/contestInfo`);

        const contestOver = new Promise((resolve) => {
            contestInfo.onmessage = (event) => {
                const message = JSON.parse(event.data.toString());
                if (message.status === "OVER" || message.status == "FINALIZED") {
                    resolve(null);
                }
            };
        });
        await contestOver;
        contestInfo.close();

        await page.goto(baseURL + "/overlay");

        for (const widgetName of simpleWidgets) {
            const showWidget = await adminApiContext.post(`./${widgetName}/show`);
            expect.soft(showWidget.ok()).toBeTruthy();
        }

        await page.waitForTimeout(overlayDisplayDelay);

        const contestName = contestConfig.replace(/\//g, "_");
        const screenshot = await page.screenshot({ path: `tests/screenshots/${contestName}.png` });

        await testInfo.attach('page', {
            body: screenshot,
            contentType: 'image/png',
        });

        for (const widgetName of simpleWidgets) {
            const hideWidget = await adminApiContext.post(`./${widgetName}/hide`);
            expect.soft(hideWidget.ok()).toBeTruthy();
        }
        await page.waitForTimeout(backendFinishCooldown);
    });
}
