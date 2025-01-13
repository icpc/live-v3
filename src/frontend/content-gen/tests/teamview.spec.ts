import test, { PlaywrightTestArgs, TestInfo, expect, request } from "@playwright/test";
import { TeamMediaType, TeamId, ExternalTeamViewSettings, Widget, ContestInfo } from "../../generated/api"
import { BACKEND_URL, contestInfo } from "./test";

const getTeamViewSettings = async (teamId: TeamId, media: TeamMediaType) => {
    const adminApiContext = await request.newContext({ baseURL: `${BACKEND_URL}/api/admin/` });
    const adminSettings: ExternalTeamViewSettings = {"mediaTypes": [media],"teamId": teamId,"showTaskStatus": false,"showAchievement": false,"showTimeLine": false};
    const resp = await adminApiContext.post("./teamView/preview", {
        data: adminSettings,
        headers: { "Content-Type": "application/json" }
    });
    expect.soft(resp.ok()).toBeTruthy();
    const widget = (await resp.json()) as Widget;
    widget.type = Widget.Type.TeamViewWidget;
    widget.location = { sizeX: 1920, sizeY: 1080, positionX: 0, positionY: 0 };
    return widget;
}

const testTeamViewOneMedia = (teamId: TeamId, media: TeamMediaType) =>
    async ({ page }: PlaywrightTestArgs, testInfo: TestInfo) => {
        const widgets = [await getTeamViewSettings(teamId, media)];
        await page.goto(`${BACKEND_URL}/overlay?forceWidgets=${encodeURIComponent(JSON.stringify(widgets))}`);

        while (true) {
            await page.waitForTimeout(500);
            const teamViewDisplay = await page.locator(".TeamViewContainer").first().evaluate((el) => {
                return window.getComputedStyle(el).getPropertyValue("display");
            });
            if (teamViewDisplay !== "none") {
                break;
            }
        }
        await page.waitForTimeout(1000);

        const testName = `${teamId}_${media}`;
        const screenshot = await page.screenshot({ path: `tests/screenshots/${testName}.png` });

        await testInfo.attach("page", {
            body: screenshot,
            contentType: "image/png",
        });
    };

test.describe("TeamViews", async () => {
    const medias = [TeamMediaType.camera, TeamMediaType.screen];
    for (let media of medias) {
        for (let team of contestInfo.teams) {
            if (team.isHidden) {
                continue;
            }
            test(`${team.id}_${media}`, testTeamViewOneMedia(team.id, media));
        }
    }
});
