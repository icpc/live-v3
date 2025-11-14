import { expect } from "@playwright/test";
import { spawn } from "child_process";
import * as path from "node:path";
import {
    backendFinishCooldown,
    backendStartCooldown,
    backendStartingPort,
    overlayDisplayDelay,
    repoDir,
    simpleLayouts,
} from "./consts.js";
import { BackendClient } from "./backend-client.js";
import { TestConfig } from "./types.js";

const spawnBackend = (contestConfig: string, port: number) => {
    const process = spawn("java", [
        "-jar",
        path.join(repoDir, "artifacts/live-v3-dev.jar"),
        "-p",
        `${port}`,
        "--no-auth",
        "-c",
        `${path.join(repoDir, contestConfig)}`,
    ]);

    process.stdout.on("data", (data) => {
        console.log(`${data}`);
    });
    process.stderr.on("data", (data) => {
        console.error(`${data}`);
    });
    process.on("close", (code) => {
        console.log(`Child process exited with code ${code}`);
    });

    return process;
};

export const generateTest = (
    index: number,
    { path: contestConfig, layouts }: TestConfig,
) => {
    return async ({ page }, testInfo) => {
        console.log(`Starting contest ${contestConfig}`);
        const port = backendStartingPort + index;

        const backendProcess = spawnBackend(contestConfig, port);
        process.on("exit", function () {
            backendProcess.kill();
        });
        process.on("SIGINT", function () {
            backendProcess.kill();
            process.exit();
        });

        const overlay = new BackendClient(port);
        await overlay.buildContext();

        await page.waitForTimeout(backendStartCooldown);
        await overlay.finalizedContestInfo();

        await page.goto(overlay.baseURL + "/overlay");

        for (const [i, l] of (layouts ?? simpleLayouts).entries()) {
            for (const widget of l.widgets) {
                expect.soft(await overlay.showWidget(widget)).toBeTruthy();
            }
            if (l.analytics?.makeFeaturedType) {
                expect
                    .soft(
                        await overlay.makeFeatured(
                            l.analytics.messageId,
                            l.analytics.makeFeaturedType,
                        ),
                    )
                    .toBeTruthy();
            }

            await page.waitForTimeout(l.displayDelay ?? overlayDisplayDelay);

            const contestName = contestConfig.replace(/\//g, "_");
            const screenshot = await page.screenshot({
                path: `tests/screenshots/${contestName}_${i}.png`,
            });

            await testInfo.attach("page", {
                body: screenshot,
                contentType: "image/png",
            });

            for (const widget of l.widgets) {
                expect.soft(await overlay.hideWidget(widget)).toBeTruthy();
            }
            if (l.analytics?.makeFeaturedType) {
                expect
                    .soft(await overlay.hideFeatured(l.analytics.messageId))
                    .toBeTruthy();
            }
        }
        await page.waitForTimeout(backendFinishCooldown);
    };
};
