import test, { ConsoleMessage, expect } from "@playwright/test";

const minutes10 = 1000 * 60 * 10;
const WIDTH = 1080;
const HEIGHT = 1920;
const visualConfig = {
  SCREEN_WIDTH: WIDTH,
  SCREEN_HEIGHT: HEIGHT,
  TICKER_SMALL_SIZE: "50%",
  TICKER_LIVE_ICON_SIZE: "70px",
  SCOREBOARD_CELL_PLACE_SIZE: "50px",
  SCOREBOARD_CELL_TEAMNAME_SIZE: "304px",
  SCOREBOARD_CELL_POINTS_SIZE: "50px",
  SCOREBOARD_CELL_PENALTY_SIZE: "92px",
  FULL_SCREEN_CLOCK_FONT_SIZE: "300px",
  FULL_SCREEN_CLOCK_COLOR: "#eeeeee33",
  BACKGROUND: "#691930"
};
const widgets = [
  {
    type: "ScoreboardWidget",
    widgetId: "scoreboard",
    location: {
      positionX: 16,
      positionY: 148,
      sizeX: 1048,
      sizeY: 1756,
    },
    statisticsId: "scoreboard",
    settings: {
      scrollDirection: "FirstPage",
      optimismLevel: "normal",
      group: "all",
    },
  },
  {
    type: "TickerWidget",
    widgetId: "ticker",
    location: {
      positionX: 540,
      positionY: 46,
      sizeX: 524,
      sizeY: 86,
    },
    statisticsId: "ticker",
    settings: {},
  },
  {
    type: "FullScreenClockWidget",
    widgetId: "fullScreenClock",
    location: {
      positionX: 16,
      positionY: 148,
      sizeX: 1048,
      sizeY: 1756,
    },
    statisticsId: "fullScreenClock",
    settings: {
      globalTimeMode: false,
      quietMode: false,
      contestCountdownMode: false,
    },
  },
];
test.describe("Instagram story", async () => {
  test.setTimeout(minutes10);
  test("Instagram story", async ({ browser }) => {
    const context = await browser.newContext({
      recordVideo: { dir: "videos/", size: { width: WIDTH, height: HEIGHT } },
      viewport: { width: WIDTH, height: HEIGHT },
    });
    const page = await context.newPage();
    // const messages: ConsoleMessage[] = [];
    // page.on('console', m => messages.push(m)); 
    const url = `localhost:8080/overlay?forceWidgets=${encodeURIComponent(
      JSON.stringify(widgets)
    )}&forceVisualConfig=${encodeURIComponent(JSON.stringify(visualConfig))}`;
    console.log(url);
    await page.goto(url);
    const selector = await page.waitForSelector(
      '[data-widget-id="scoreboard"]'
    );
    const locator = await page.locator('[data-widget-id="scoreboard"]');
    await page.waitForTimeout(4 * 60 * 1000 + 15 * 1000);
    // await expect(locator).toHaveText('OVER', { timeout: 100000000})
    await context.close();
    // expect(messages).toEqual([]);
  });
});
