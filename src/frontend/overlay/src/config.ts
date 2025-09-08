import { setFavicon } from "@shared/setFavicon";
import { isShouldUseDarkColor } from "@/utils/colors";
import { faviconTemplate } from "@/consts";
import { LocationRectangle } from "@/utils/location-rectangle";

const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
const WS_PORT = import.meta.env.VITE_WEBSOCKET_PORT ?? window.location.port;
const VISUAL_CONFIG_URL = import.meta.env.VITE_VISUAL_CONFIG_URL ?? `${window.location.protocol}//${window.location.hostname}:${WS_PORT}/api/overlay/visualConfig.json`;

const urlParams = new URLSearchParams(window.location.search);
const queryVisualConfig = JSON.parse(urlParams.get("forceVisualConfig") ?? "{}");

const visualConfig = await fetch(VISUAL_CONFIG_URL)
    .then(r => r.json())
    .catch((e) => console.error("failed to load visual config: " + e)) ?? {};


function Location(positionX: number, positionY: number, sizeX: number, sizeY: number): LocationRectangle {
    return Object.freeze({
        positionX: positionX,
        positionY: positionY,
        sizeX: sizeX,
        sizeY: sizeY
    });
}
/**
 * Creates a Proxy-wrapped configuration object that can manage nested overrides.
 *
 * @param {Record<string, any>} override - An object containing override values for configuration keys.
 * @returns {Record<string, any>} A proxy object that supports reading and writing nested properties,
 *                                including dotted paths like "a.b.c".
 *
 * **How it works**:
 * - On `get`: if `target[key]` is not found, it checks if the key is dotted (e.g. "a.b").
 *   If yes, it extracts the prefix ("a"), looks for any sub-object proxy, and then delegates the remainder ("b") to that sub-proxy.
 * - On `set`: if the value being set is an object, it recursively wraps it in another proxy, merging any override values that match the dotted path prefix.
 *
 * **Usage example**:
 * ```js
 * const config = createProxy({
 *   "featureA.enabled": true,
 *   "featureB.options": { debug: false }
 * });
 *
 * // Accessing a nested path via dot-notation:
 * config["featureA.enabled"] // true
 *
 * // Setting a nested object:
 * config.featureB = { options: { debug: true, logLevel: 2 } };
 *
 * // Now config.featureB.options will return { debug: true, logLevel: 2 }
 * ```
 *
 * **Potential errors**:
 * - If you pass in non-object overrides or non-serializable data, it may behave unexpectedly.
 * - Dot notation relies on the first segment being used as the 'prefix' for the sub-object. Make sure your keys are well-formed.
 */
function createProxy(
    override,
    // No known way to infer types from assignment yet.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): Record<string, any> {
    const subObjects = {};
    return new Proxy({}, {
        get(target, key) {
            const r = target[key];
            if (r !== null && r !== undefined) return r;
            const prefix = (key as string).split(".", 1)[0];
            if (prefix in subObjects) {
                return target[prefix][(key as string).substring(prefix.length + 1)];
            }
            return undefined;
        },
        set(target, key: string, value) {
            if (typeof value === "object" && value !== null && value !== undefined) {
                subObjects[key] = true;
                const newOverride = override[key] ?? {};
                Object.entries(override).forEach(([k, v]) => {
                    if (k.startsWith(`${key}.`)) {
                        newOverride[k.substring(key.length + 1)] = v;
                    }
                });
                const subProxy = createProxy(newOverride);
                target[key] = subProxy;
                Object.entries(value).forEach(([k, v]) => {
                    subProxy[k] = v;
                });
                return true;
            }
            target[key] = override[key] ?? value;
            return true;
        }
    });
}

const config = createProxy(
    {
        ...visualConfig,
        ...queryVisualConfig,
    },
);

config.CONTEST_COLOR = "#4C83C3";
config.CONTEST_CAPTION = "";

config.BASE_URL_WS = (import.meta.env.VITE_WEBSOCKET_URL ?? WS_PROTO + window.location.hostname + ":" + WS_PORT + "/api/overlay");

// Non Styling configs
config.WEBSOCKET_RECONNECT_TIME = 5000; // ms

// Strings
config.QUEUE_TITLE = "Queue";
config.QUEUE_CAPTION = config.CONTEST_CAPTION;
config.SCOREBOARD_CAPTION = config.CONTEST_CAPTION;
config.STATISTICS_TITLE = "Statistics";
config.STATISTICS_CAPTION = config.CONTEST_CAPTION;

// Behaviour
config.TICKER_SCOREBOARD_REPEATS = 1;
config.QUEUE_MAX_ROWS = 20;
config.QUEUE_HORIZONTAL_HEIGHT_NUM = 5;

// Timings
config.WIDGET_TRANSITION_TIME = 300; // ms
config.QUEUE_ROW_TRANSITION_TIME = 700; // ms
config.QUEUE_ROW_APPEAR_TIME = config.QUEUE_ROW_TRANSITION_TIME; // ms
config.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME = 500; // ms
config.QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY = 5000; // ms
config.QUEUE_ROW_FTS_TRANSITION_TIME = 3000; // ms
config.SCOREBOARD_ROW_TRANSITION_TIME = 1000; // ms
config.SCOREBOARD_SCROLL_INTERVAL = 20000; // ms
config.PICTURES_APPEAR_TIME = 1000; // ms
config.SVG_APPEAR_TIME = 1000; // ms
config.VIDEO_APPEAR_TIME = 100; // ms
config.TEAM_VIEW_APPEAR_TIME = 1000; // ms
config.PVP_APPEAR_TIME = 1000; // ms
config.TICKER_SCROLL_TRANSITION_TIME = 1000; //ms
config.TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME = 300; //ms
config.STATISTICS_CELL_MORPH_TIME = 200; //ms
config.CELL_FLASH_PERIOD = 500; //ms

// Styles > Global
config.GLOBAL_DEFAULT_FONT_FAMILY = "Helvetica, serif"; // css-property
config.GLOBAL_DEFAULT_FONT_SIZE = "22px"; // css-property
config.GLOBAL_DEFAULT_FONT_WEIGHT = 400; // css-property
config.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD = 700; // css-property
config.GLOBAL_DEFAULT_FONT = config.GLOBAL_DEFAULT_FONT_SIZE + " " + config.GLOBAL_DEFAULT_FONT_FAMILY; // css property MUST HAVE FONT SIZE
config.GLOBAL_BACKGROUND_COLOR = "#242425";
config.GLOBAL_TEXT_COLOR = "#FFF";
config.GLOBAL_BORDER_RADIUS = "16px";

// Extra CSS configurations
config.EXTRA_CSS = null; // Additional CSS that can be injected

config.VERDICT_OK = "#3bba6b";
config.VERDICT_NOK = "#CB2E28";
config.VERDICT_UNKNOWN = "#F3BE4B";

// Styles > Scoreboard
config.SCOREBOARD_BACKGROUND_COLOR = config.GLOBAL_BACKGROUND_COLOR;
config.SCOREBOARD_BORDER_RADIUS = config.GLOBAL_BORDER_RADIUS;
config.SCOREBOARD_TEXT_COLOR = config.GLOBAL_TEXT_COLOR;
config.SCOREBOARD_CAPTION_FONT_SIZE = "32px"; // css value
config.SCOREBOARD_HEADER_BACKGROUND_COLOR = config.CONTEST_COLOR;
config.SCOREBOARD_HEADER_DIVIDER_COLOR = config.SCOREBOARD_BACKGROUND_COLOR;
config.SCOREBOARD_HEADER_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;
config.SCOREBOARD_HEADER_FONT_WEIGHT = config.GLOBAL_DEFAULT_FONT_WEIGHT;
config.SCOREBOARD_HEADER_HEIGHT = 38;
config.SCOREBOARD_ROWS_DIVIDER_COLOR = config.CONTEST_COLOR;
config.SCOREBOARD_ROW_HEIGHT = 32; // px // todo: tweek this
config.SCOREBOARD_ROW_PADDING = 1; // px
config.SCOREBOARD_BETWEEN_HEADER_PADDING = 3; //px
config.SCOREBOARD_ROW_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;
// config.SCOREBOARD_TABLE_ROW_FONT_WEIGHT = 700;

config.SCOREBOARD_CELL_PLACE_SIZE = "73px";
config.SCOREBOARD_CELL_TEAMNAME_SIZE = "304px";
config.SCOREBOARD_CELL_TEAMNANE_ALIGN = "left";
config.SCOREBOARD_CELL_POINTS_SIZE = "81px";
config.SCOREBOARD_CELL_POINTS_ALIGN = "center";
config.SCOREBOARD_CELL_PENALTY_SIZE = "92px";
config.SCOREBOARD_CELL_PENALTY_ALIGN = "center";

config.SCOREBOARD_NORMAL_NAME = "Current";
config.SCOREBOARD_OPTIMISTIC_NAME = "Optimistic";
config.SCOREBOARD_PESSIMISTIC_NAME = "Pessimistic";
config.SCOREBUARD_UNDEFINED_NAME = "??";
config.SCOREBOARD_STANDINGS_NAME = "standings";


config.QUEUE_ROW_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;
config.QUEUE_ROW_BACKGROUND = "rgba(0, 0, 0, 0.08)";
config.QUEUE_ROW_HEIGHT = 32; // px
config.QUEUE_ROW_WIDTH = 368; // px
config.QUEUE_ROW_Y_PADDING = 1; // px
config.QUEUE_HORIZONTAL_ROW_Y_PADDING = 3; // px
config.QUEUE_ROW_FEATURED_RUN_PADDING = 3; // px
config.QUEUE_WRAP_PADDING = 8; // px
config.QUEUE_HORIZONTAL_ROW_X_PADDING = config.QUEUE_WRAP_PADDING; // px
config.QUEUE_OPACITY = 0.95;
config.QUEUE_FEATURED_RUN_ASPECT = 16 / 9;
config.QUEUE_BACKGROUND_COLOR = config.CONTEST_COLOR;
config.QUEUE_HORIZONTAL_BACKGROUND_COLOR = config.QUEUE_BACKGROUND_COLOR;
config.QUEUE_ROW_PROBLEM_LABEL_WIDTH = 28; // px
config.QUEUE_HEADER_FONT_SIZE = "32px";
config.QUEUE_HEADER_LINE_HEIGHT = "44px";

config.SCORE_NONE_TEXT = ".";

config.STATISTICS_TITLE_FONT_SIZE = "30px";
config.STATISTICS_OPACITY = 0.95;
config.STATISTICS_BG_COLOR = "#000000";
config.STATISTICS_TITLE_COLOR = "#FFFFFF";
config.STATISTICS_STATS_VALUE_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;
config.STATISTICS_STATS_VALUE_FONT_FAMILY = config.GLOBAL_DEFAULT_FONT_FAMILY;
config.STATISTICS_STATS_VALUE_COLOR = "#FFFFFF";
config.STATISTICS_BAR_HEIGHT_PX = 24;
config.STATISTICS_BAR_HEIGHT = `${config.STATISTICS_BAR_HEIGHT_PX}px`;
config.STATISTICS_BAR_GAP_PX = 9;
config.STATISTICS_BAR_GAP = `${config.STATISTICS_BAR_GAP_PX}px`;


// TODO: remove
config.CELL_FONT_FAMILY = config.GLOBAL_DEFAULT_FONT_FAMILY;
config.CELL_FONT_SIZE = "18px";
config.CELL_TEXT_COLOR = "#FFFFFF";
config.CELL_TEXT_COLOR_INVERSE = "#000000";
config.CELL_BG_COLOR = "#000000";
config.CELL_BG_COLOR_ODD = "rgba(1; 1, 1, 0.9)";
config.CELL_BG_COLOR2 = "#1E1E1E";
config.CELL_BG_COLOR_ODD2 = "#242424";

config.CELL_PROBLEM_LINE_WIDTH = "5px"; // css property
config.CELL_QUEUE_VERDICT_WIDTH = "80px"; // css property
config.CELL_QUEUE_VERDICT_WIDTH2 = "20px"; // css property
config.CELL_QUEUE_RANK_WIDTH = "50px"; // css property
config.CELL_QUEUE_RANK_WIDTH2 = "30px"; // css property
config.CELL_QUEUE_TOTAL_SCORE_WIDTH = "50px"; // css property
config.CELL_QUEUE_TASK_WIDTH = "50px"; // css property

config.CELL_NAME_LEFT_PADDING = "5px"; // css property
config.CELL_NAME_RIGHT_PADDING = config.CELL_NAME_LEFT_PADDING; // css property

config.TICKER_SMALL_SIZE = "12%"; // css property
config.TICKER_SMALL_BACKGROUND = config.VERDICT_NOK;
config.TICKER_BACKGROUND = config.SCOREBOARD_BACKGROUND_COLOR;
config.TICKER_DEFAULT_SCOREBOARD_BACKGROUND = "#FFFFFF00";
config.TICKER_OPACITY = 0.95;
config.TICKER_FONT_COLOR = "#FFFFFF";
config.TICKER_FONT_FAMILY = config.GLOBAL_DEFAULT_FONT_FAMILY;
config.TICKER_TEXT_FONT_SIZE = "32px"; // css property
config.TICKER_TEXT_MARGIN_LEFT = "16px"; // css property
config.TICKER_CLOCK_FONT_SIZE = "32px"; // css property
config.TICKER_CLOCK_MARGIN_LEFT = "10px"; // css property
config.TICKER_SCOREBOARD_RANK_WIDTH = "50px"; // css property
config.TICKER_LIVE_ICON_SIZE = "32px";

config.PICTURE_NAME_BACKGROUND_COLOR = config.CONTEST_COLOR;
config.PICTURE_NAME_FONT_COLOR = "#FFFFFF";
config.PICTURE_NAME_FONT_SIZE = "22pt";
config.PICTURE_BORDER_SIZE = "5px";
config.PICTURE_NAME_FONT_FAMILY = config.GLOBAL_DEFAULT_FONT_FAMILY;


// not used
config.TEAMVIEW_SMALL_FACTOR = "50%"; // css property

config.FULL_SCREEN_CLOCK_CENTERED = true;
config.FULL_SCREEN_CLOCK_PADDING_TOP = "240px";
config.FULL_SCREEN_CLOCK_FONT_SIZE = "400px";
config.FULL_SCREEN_CLOCK_COLOR = "#eeeeee";
config.FULL_SCREEN_CLOCK_FONT_FAMILY = "Helvetica; monospace";

config.ADVERTISEMENT_BACKGROUND = "#FFFFFF"; // hex value.
config.ADVERTISEMENT_COLOR = isShouldUseDarkColor(config.ADVERTISEMENT_BACKGROUND) ? "black" : "white";
config.ADVERTISEMENT_FONT_FAMILY = config.GLOBAL_DEFAULT_FONT_FAMILY;

config.STAR_SIZE = 33; // px

config.QUEUE_PROBLEM_LABEL_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;

// Medals
config.MEDAL_COLORS = Object.freeze({
    "gold": "#F9A80D",
    "silver": "#ACACAC",
    "bronze": "#E27B5A"
});

// Debug Behaviour
config.LOG_LINES = 300;

config.CONTESTER_ROW_OPACITY = 0.95;

config.CONTESTER_FONT_SIZE = config.GLOBAL_DEFAULT_FONT_SIZE;
config.CONTESTER_BACKGROUND_COLOR = config.CONTEST_COLOR;

config.CONTESTER_ROW_BORDER_RADIUS = config.GLOBAL_BORDER_RADIUS;
config.CONTESTER_ROW_HEIGHT = "32px";
config.CONTESTER_NAME_WIDTH = "150px";
config.CONTESTER_INFO_SCORE_WIDTH = "51px";
config.CONTESTER_INFO_RANK_WIDTH = "32px";

config.TIMELINE_ELEMENT_DIAMETER = 25;
config.TIMELINE_BORDER_RADIUS = config.GLOBAL_BORDER_RADIUS;
config.TIMELINE_LINE_HEIGHT = 4;
config.TIMELINE_WRAP_HEIGHT = 148;
config.TIMELINE_TIMEBORDER_COLOR = "#FFF";
config.TIMELINE_REAL_WIDTH = 0.97;
config.TIMELINE_PADDING = 10;
config.TIMELINE_LEFT_TIME_PADDING = 20;
config.TIMELINE_ANIMATION_TIME = 10000; // ms

config.TIMELINE_WRAP_HEIGHT_PVP = 67;
config.TIMELINE_ELEMENT_DIAMETER_PVP = 20;
config.TIMELINE_PADDING_PVP = 5;
config.TIMELINE_REAL_WIDTH_PVP = 0.93;


config.KEYLOG_MAXIMUM_FOR_NORMALIZATION = 500; // max value for normalization
config.KEYLOG_BUCKET_SIZE_MS = 60 * 1000; // 5 minutes in milliseconds
config.KEYLOG_TOP_PADDING = 6; // px
config.KEYLOG_BOTTOM_PADDING = 2; // px
config.KEYLOG_Z_INDEX = 0;

config.KEYLOG_ANIMATION_DURATION = 800; // ms
config.KEYLOG_ANIMATION_EASING = "ease-out";

config.KEYLOG_STROKE_WIDTH = 1.5; // px

config.KEYLOG_STROKE_DARK = "#000000A6";
config.KEYLOG_STROKE_LIGHT = "#FFFFFFBF";
config.KEYLOG_FILL_DARK = "#0000001F";
config.KEYLOG_FILL_LIGHT = "#FFFFFF29";


config.TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR = 0.39;
config.SPLITSCREEN_SECONDARY_FACTOR = config.TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR;

// config.PVP_OPACITY = 0.95;
// config.TEAM_VIEW_OPACITY = 0.95;
config.PVP_BACKGROUND = config.CONTESTER_BACKGROUND_COLOR;
config.PVP_TABLE_ROW_HEIGHT = 32;
config.PVP_TEAM_STATUS_TASK_WIDTH = 50;

config.CIRCLE_PROBLEM_SIZE = "28px";
config.CIRCLE_PROBLEM_LINE_WIDTH = "3.5px";

config.CELL_INFO_VERDICT_WIDTH = "100px"; // css property

// layers (z-indexes)
config.QUEUE_BASIC_ZINDEX = 20;

config.LOCATOR_MAGIC_CONSTANT = 343;

config.WEBSOCKETS = {
    mainScreen: `${config.BASE_URL_WS}/mainScreen`,
    contestInfo: `${config.BASE_URL_WS}/contestInfo`,
    queue: `${config.BASE_URL_WS}/queue`,
    statistics: `${config.BASE_URL_WS}/statistics`,
    ticker: `${config.BASE_URL_WS}/ticker`,
    scoreboardNormal: `${config.BASE_URL_WS}/scoreboard/normal`,
    scoreboardOptimistic: `${config.BASE_URL_WS}/scoreboard/optimistic`,
    scoreboardPessimistic: `${config.BASE_URL_WS}/scoreboard/pessimistic`,
};
config.WIDGET_POSITIONS = {
    advertisement: Location(16, 16, 1488, 984),
    picture: Location(16, 16, 1488, 984),
    svg: Location(0, 0, 1920, 1080),
    queue: Location(1520, 248, 384, 752),
    scoreboard: Location(16, 16, 1488, 984),
    statistics: Location(16, 662, 1488, 338),
    ticker: Location(16, 1016, 1888, 48),
    fullScreenClock: Location(16, 16, 1488, 984),
    teamLocator: Location(0, 0, 1920, 1080),
    teamview: {
        SINGLE: Location(16, 16, 1488, 984),
        PVP_TOP: Location(16, 16, 1488, 984 / 2 + 16),
        PVP_BOTTOM: Location(16, 16 + 984 / 2 - 16, 1488, 984 / 2 + 16),
        TOP_LEFT: Location(16, 16, 1488 / 2, 837 / 2),
        TOP_RIGHT: Location(16 + 1488 / 2, 16, 1488 / 2, 837 / 2),
        BOTTOM_LEFT: Location(16, 16 + 837 / 2, 1488 / 2, 837 / 2),
        BOTTOM_RIGHT: Location(16 + 1488 / 2, 16 + 837 / 2, 1488 / 2, 837 / 2),
    },
    ...queryVisualConfig.WIDGET_POSITIONS
};

setFavicon(faviconTemplate
    .replaceAll("{CONTEST_COLOR}", config["CONTEST_COLOR"])
    .replaceAll("{TEXT_COLOR}", isShouldUseDarkColor(config["CONTEST_COLOR"]) ? "#000000" : "#FFFFFF"));

export default config;
