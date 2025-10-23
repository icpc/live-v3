import { setFavicon } from "@shared/setFavicon";
import { isShouldUseDarkColor } from "@/utils/colors";
import { faviconTemplate } from "@/consts";
import { LocationRectangle } from "@/utils/location-rectangle";
import type { OverlayConfig } from "./config.interface";

const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
const WS_PORT = import.meta.env.VITE_WEBSOCKET_PORT ?? window.location.port;
const VISUAL_CONFIG_URL = import.meta.env.VITE_VISUAL_CONFIG_URL ?? `${window.location.protocol}//${window.location.hostname}:${WS_PORT}/api/overlay/visualConfig.json`;

const urlParams = new URLSearchParams(window.location.search);
const queryVisualConfig = JSON.parse(urlParams.get("forceVisualConfig") ?? "{}");

const visualConfig = await fetch(VISUAL_CONFIG_URL)
    .then(r => r.json())
    .catch((e) => console.error("failed to load visual config: " + e)) ?? {};


function Location(positionX: number, positionY: number, sizeX: number, sizeY: number): LocationRectangle {
    return {
        positionX: positionX,
        positionY: positionY,
        sizeX: sizeX,
        sizeY: sizeY
    };
}
function getDefaultConfig(): EvaluatableTo<OverlayConfig> {
    return {
        CONTEST_COLOR: "#4C83C3",
        CONTEST_CAPTION: "",
        BASE_URL_WS: (import.meta.env.VITE_WEBSOCKET_URL ?? WS_PROTO + window.location.hostname + ":" + WS_PORT + "/api/overlay"),

        // Non Styling configs
        WEBSOCKET_RECONNECT_TIME: 5000, // ms

        // Strings
        QUEUE_TITLE: "Queue",
        QUEUE_CAPTION: (cfg: OverlayConfig) => cfg.CONTEST_CAPTION,
        SCOREBOARD_CAPTION: (cfg: OverlayConfig) => cfg.CONTEST_CAPTION,
        STATISTICS_TITLE: "Statistics",
        STATISTICS_CAPTION: (cfg: OverlayConfig) => cfg.CONTEST_CAPTION,

        // Behaviour
        TICKER_SCOREBOARD_REPEATS: 1,
        QUEUE_MAX_ROWS: 20,
        QUEUE_HORIZONTAL_HEIGHT_NUM: 5,

        // Timings
        WIDGET_TRANSITION_TIME: 300, // ms
        QUEUE_ROW_TRANSITION_TIME: 700, // ms
        QUEUE_ROW_APPEAR_TIME: (cfg: OverlayConfig) => cfg.QUEUE_ROW_TRANSITION_TIME, // ms
        QUEUE_ROW_FEATURED_RUN_APPEAR_TIME: 500, // ms
        QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY: 5000, // ms
        QUEUE_ROW_FTS_TRANSITION_TIME: 3000, // ms
        SCOREBOARD_ROW_TRANSITION_TIME: 1000, // ms
        SCOREBOARD_SCROLL_INTERVAL: 20000, // ms
        PICTURES_APPEAR_TIME: 1000, // ms
        SVG_APPEAR_TIME: 1000, // ms
        VIDEO_APPEAR_TIME: 100, // ms
        TEAM_VIEW_APPEAR_TIME: 1000, // ms
        PVP_APPEAR_TIME: 1000, // ms
        TICKER_SCROLL_TRANSITION_TIME: 1000, //ms
        TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME: 300, //ms
        STATISTICS_CELL_MORPH_TIME: 200, //ms
        CELL_FLASH_PERIOD: 500, //ms

        // Styles > Global
        GLOBAL_DEFAULT_FONT_FAMILY: "Helvetica, serif", // css-property
        GLOBAL_DEFAULT_FONT_SIZE: "22px", // css-property
        GLOBAL_DEFAULT_FONT_WEIGHT: 400, // css-property
        GLOBAL_DEFAULT_FONT_WEIGHT_BOLD: 700, // css-property
        GLOBAL_DEFAULT_FONT: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE + " " + cfg.GLOBAL_DEFAULT_FONT_FAMILY, // MUST HAVE FONT SIZE
        GLOBAL_BACKGROUND_COLOR: "#242425",
        GLOBAL_TEXT_COLOR: "#FFF",
        GLOBAL_BORDER_RADIUS: "16px",

        // Extra CSS configurations
        EXTRA_CSS: null, // Additional CSS that can be injected

        VERDICT_OK: "#3bba6b",
        VERDICT_NOK: "#CB2E28",
        VERDICT_UNKNOWN: "#F3BE4B",

        // Styles > Scoreboard
        SCOREBOARD_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.GLOBAL_BACKGROUND_COLOR,
        SCOREBOARD_BORDER_RADIUS: (cfg: OverlayConfig) => cfg.GLOBAL_BORDER_RADIUS,
        SCOREBOARD_TEXT_COLOR: (cfg: OverlayConfig) => cfg.GLOBAL_TEXT_COLOR,
        SCOREBOARD_CAPTION_FONT_SIZE: "32px", // css value
        SCOREBOARD_HEADER_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.CONTEST_COLOR,
        SCOREBOARD_HEADER_DIVIDER_COLOR: (cfg: OverlayConfig) => cfg.SCOREBOARD_BACKGROUND_COLOR,
        SCOREBOARD_HEADER_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,
        SCOREBOARD_HEADER_FONT_WEIGHT: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_WEIGHT,
        SCOREBOARD_HEADER_HEIGHT: 38,
        SCOREBOARD_ROWS_DIVIDER_COLOR: (cfg: OverlayConfig) => cfg.CONTEST_COLOR,
        SCOREBOARD_ROW_HEIGHT: 32, // px // todo: tweak this
        SCOREBOARD_ROW_PADDING: 1, // px
        SCOREBOARD_BETWEEN_HEADER_PADDING: 3, //px
        SCOREBOARD_ROW_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,
        SCOREBOARD_TABLE_ROW_FONT_WEIGHT: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_WEIGHT,

        SCOREBOARD_CELL_PLACE_SIZE: "73px",
        SCOREBOARD_CELL_TEAMNAME_SIZE: "304px",
        SCOREBOARD_CELL_TEAMNANE_ALIGN: "left",
        SCOREBOARD_CELL_POINTS_SIZE: "81px",
        SCOREBOARD_CELL_POINTS_ALIGN: "center",
        SCOREBOARD_CELL_PENALTY_SIZE: "92px",
        SCOREBOARD_CELL_PENALTY_ALIGN: "center",

        SCOREBOARD_NORMAL_NAME: "Current",
        SCOREBOARD_OPTIMISTIC_NAME: "Optimistic",
        SCOREBOARD_PESSIMISTIC_NAME: "Pessimistic",
        SCOREBOARD_UNDEFINED_NAME: "??",
        SCOREBOARD_STANDINGS_NAME: "standings",


        QUEUE_ROW_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,
        QUEUE_ROW_BACKGROUND: "rgba(0, 0, 0, 0.08)",
        QUEUE_ROW_HEIGHT: 32, // px
        QUEUE_ROW_WIDTH: 368, // px
        QUEUE_ROW_Y_PADDING: 1, // px
        QUEUE_HORIZONTAL_ROW_Y_PADDING: 3, // px
        QUEUE_ROW_FEATURED_RUN_PADDING: 3, // px
        QUEUE_WRAP_PADDING: 8, // px
        QUEUE_HORIZONTAL_ROW_X_PADDING: (cfg: OverlayConfig) => cfg.QUEUE_WRAP_PADDING, // px
        QUEUE_OPACITY: 0.95,
        QUEUE_FEATURED_RUN_ASPECT: 16 / 9,
        QUEUE_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.CONTEST_COLOR,
        QUEUE_HORIZONTAL_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.QUEUE_BACKGROUND_COLOR,
        QUEUE_ROW_PROBLEM_LABEL_WIDTH: 28, // px
        QUEUE_HEADER_FONT_SIZE: "32px",
        QUEUE_HEADER_LINE_HEIGHT: "44px",

        SCORE_NONE_TEXT: ".",

        STATISTICS_TITLE_FONT_SIZE: "30px",
        STATISTICS_OPACITY: 0.95,
        STATISTICS_BG_COLOR: "#000000",
        STATISTICS_TITLE_COLOR: "#FFFFFF",
        STATISTICS_STATS_VALUE_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,
        STATISTICS_STATS_VALUE_FONT_FAMILY: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_FAMILY,
        STATISTICS_STATS_VALUE_COLOR: "#FFFFFF",
        STATISTICS_BAR_HEIGHT_PX: 24,
        STATISTICS_BAR_HEIGHT: (cfg: OverlayConfig) => `${cfg.STATISTICS_BAR_HEIGHT_PX}px`,
        STATISTICS_BAR_GAP_PX: 9,
        STATISTICS_BAR_GAP: (cfg: OverlayConfig) => `${cfg.STATISTICS_BAR_GAP_PX}px`,


        // TODO: remove
        CELL_FONT_FAMILY: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_FAMILY,
        CELL_FONT_SIZE: "18px",
        CELL_TEXT_COLOR: "#FFFFFF",
        CELL_TEXT_COLOR_INVERSE: "#000000",
        CELL_BG_COLOR: "#000000",
        CELL_BG_COLOR_ODD: "rgba(1; 1, 1, 0.9)",
        CELL_BG_COLOR2: "#1E1E1E",
        CELL_BG_COLOR_ODD2: "#242424",

        CELL_PROBLEM_LINE_WIDTH: "5px", // css property
        CELL_QUEUE_VERDICT_WIDTH: "80px", // css property
        CELL_QUEUE_VERDICT_WIDTH2: "20px", // css property
        CELL_QUEUE_RANK_WIDTH: "50px", // css property
        CELL_QUEUE_RANK_WIDTH2: "30px", // css property
        CELL_QUEUE_TOTAL_SCORE_WIDTH: "50px", // css property
        CELL_QUEUE_TASK_WIDTH: "50px", // css property

        CELL_NAME_LEFT_PADDING: "5px", // css property
        CELL_NAME_RIGHT_PADDING: (cfg: OverlayConfig) => cfg.CELL_NAME_LEFT_PADDING, // css property

        TICKER_SMALL_SIZE: "12%", // css property
        TICKER_SMALL_BACKGROUND: (cfg: OverlayConfig) => cfg.VERDICT_NOK,
        TICKER_BACKGROUND: (cfg: OverlayConfig) => cfg.SCOREBOARD_BACKGROUND_COLOR,
        TICKER_DEFAULT_SCOREBOARD_BACKGROUND: "#FFFFFF00",
        TICKER_OPACITY: 0.95,
        TICKER_FONT_COLOR: "#FFFFFF",
        TICKER_FONT_FAMILY: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_FAMILY,
        TICKER_TEXT_FONT_SIZE: "32px", // css property
        TICKER_TEXT_MARGIN_LEFT: "16px", // css property
        TICKER_CLOCK_FONT_SIZE: "32px", // css property
        TICKER_CLOCK_MARGIN_LEFT: "10px", // css property
        TICKER_SCOREBOARD_RANK_WIDTH: "50px", // css property
        TICKER_LIVE_ICON_SIZE: "32px",

        PICTURE_NAME_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.CONTEST_COLOR,
        PICTURE_NAME_FONT_COLOR: "#FFFFFF",
        PICTURE_NAME_FONT_SIZE: "22pt",
        PICTURE_BORDER_SIZE: "5px",
        PICTURE_NAME_FONT_FAMILY: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_FAMILY,


        // not used
        TEAMVIEW_SMALL_FACTOR: "50%", // css property

        FULL_SCREEN_CLOCK_CENTERED: true,
        FULL_SCREEN_CLOCK_PADDING_TOP: "240px",
        FULL_SCREEN_CLOCK_FONT_SIZE: "400px",
        FULL_SCREEN_CLOCK_COLOR: "#eeeeee",
        FULL_SCREEN_CLOCK_FONT_FAMILY: "Helvetica; monospace",

        ADVERTISEMENT_BACKGROUND: "#FFFFFF", // hex value.
        ADVERTISEMENT_COLOR: (cfg: OverlayConfig) => isShouldUseDarkColor(cfg.ADVERTISEMENT_BACKGROUND) ? "black" : "white",
        ADVERTISEMENT_FONT_FAMILY: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_FAMILY,

        STAR_SIZE: 33, // px

        QUEUE_PROBLEM_LABEL_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,

        // Medals
        MEDAL_COLORS: {
            gold: "#F9A80D",
            silver: "#ACACAC",
            bronze: "#E27B5A"
        },

        // Debug Behaviour
        LOG_LINES: 300,

        CONTESTER_ROW_OPACITY: 0.95,

        CONTESTER_FONT_SIZE: (cfg: OverlayConfig) => cfg.GLOBAL_DEFAULT_FONT_SIZE,
        CONTESTER_BACKGROUND_COLOR: (cfg: OverlayConfig) => cfg.CONTEST_COLOR,

        CONTESTER_ROW_BORDER_RADIUS: (cfg: OverlayConfig) => cfg.GLOBAL_BORDER_RADIUS,
        CONTESTER_ROW_HEIGHT: "32px",
        CONTESTER_NAME_WIDTH: "150px",
        CONTESTER_INFO_SCORE_WIDTH: "51px",
        CONTESTER_INFO_RANK_WIDTH: "32px",

        TIMELINE_ELEMENT_DIAMETER: 25,
        TIMELINE_BORDER_RADIUS: (cfg: OverlayConfig) => cfg.GLOBAL_BORDER_RADIUS,
        TIMELINE_LINE_HEIGHT: 4,
        TIMELINE_WRAP_HEIGHT: 148,
        TIMELINE_TIMEBORDER_COLOR: "#FFF",
        TIMELINE_REAL_WIDTH: 0.97,
        TIMELINE_PADDING: 10,
        TIMELINE_LEFT_TIME_PADDING: 20,
        TIMELINE_ANIMATION_TIME: 10000, // ms

        TIMELINE_WRAP_HEIGHT_PVP: 67,
        TIMELINE_ELEMENT_DIAMETER_PVP: 20,
        TIMELINE_PADDING_PVP: 5,
        TIMELINE_REAL_WIDTH_PVP: 0.93,


        KEYLOG_MAXIMUM_FOR_NORMALIZATION: 500, // max value for normalization
        KEYLOG_TOP_PADDING: 6, // px
        KEYLOG_BOTTOM_PADDING: 2, // px
        KEYLOG_Z_INDEX: 0,

        KEYLOG_ANIMATION_DURATION: 800, // ms
        KEYLOG_ANIMATION_EASING: "ease-out",

        KEYLOG_STROKE_WIDTH: 1.5, // px

        KEYLOG_STROKE_DARK: "#000000A6",
        KEYLOG_STROKE_LIGHT: "#FFFFFFBF",
        KEYLOG_FILL_DARK: "#0000001F",
        KEYLOG_FILL_LIGHT: "#FFFFFF29",
        KEYLOG_GLOW_BLUR: 0,


        TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR: 0.39,
        SPLITSCREEN_SECONDARY_FACTOR: (cfg: OverlayConfig) => cfg.TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR,

        // PVP_OPACITY: 0.95,
        // TEAM_VIEW_OPACITY: 0.95,
        PVP_BACKGROUND: (cfg: OverlayConfig) => cfg.CONTESTER_BACKGROUND_COLOR,
        PVP_TABLE_ROW_HEIGHT: 32,
        PVP_TEAM_STATUS_TASK_WIDTH: 50,

        CIRCLE_PROBLEM_SIZE: "28px",
        CIRCLE_PROBLEM_LINE_WIDTH: "3.5px",

        CELL_INFO_VERDICT_WIDTH: "100px", // css property

        // layers (z-indexes)
        QUEUE_BASIC_ZINDEX: 20,

        LOCATOR_MAGIC_CONSTANT: 343,

        WEBSOCKETS: (cfg: OverlayConfig) => ({
            mainScreen: `${cfg.BASE_URL_WS}/mainScreen`,
            contestInfo: `${cfg.BASE_URL_WS}/contestInfo`,
            queue: `${cfg.BASE_URL_WS}/queue`,
            statistics: `${cfg.BASE_URL_WS}/statistics`,
            ticker: `${cfg.BASE_URL_WS}/ticker`,
            scoreboardNormal: `${cfg.BASE_URL_WS}/scoreboard/normal`,
            scoreboardOptimistic: `${cfg.BASE_URL_WS}/scoreboard/optimistic`,
            scoreboardPessimistic: `${cfg.BASE_URL_WS}/scoreboard/pessimistic`,
        }),
        WIDGET_POSITIONS: {
            advertisement: Location(16, 16, 1488, 984),
            picture: Location(16, 16, 1488, 984),
            svg: Location(0, 0, 1920, 1080),
            queue: Location(1520, 248, 384, 752),
            scoreboard: Location(16, 16, 1488, 984),
            statistics: Location(16, 662, 1488, 338),
            ticker: Location(16, 1016, 1888, 48),
            fullScreenClock: Location(16, 16, 1488, 984),
            teamLocator: Location(0, 0, 1920, 1080),
            teamViewSingle: Location(16, 16, 1488, 984),
            teamViewPvpTop: Location(16, 16, 1488, 984 / 2 + 16),
            teamViewPvpBottom: Location(16, 16 + 984 / 2 - 16, 1488, 984 / 2 + 16),
            teamViewTopLeft: Location(16, 16, 1488 / 2, 837 / 2),
            teamViewTopRight: Location(16 + 1488 / 2, 16, 1488 / 2, 837 / 2),
            teamViewBottomLeft: Location(16, 16 + 837 / 2, 1488 / 2, 837 / 2),
            teamViewBottomRight: Location(16 + 1488 / 2, 16 + 837 / 2, 1488 / 2, 837 / 2),
        },
        ADMIN_HIDE_CONTROL: [],
        ADMIN_HIDE_MENU: []
    };
}

const defaultConfig = getDefaultConfig();

type EvaluatableTo<T> = {
    [K in keyof T]: T[K] | ((T) => T[K])
}

function merge<T>(defaultConfig: EvaluatableTo<T>, serverVisualConfig: object, queryVisualConfig: object): T {
    const result = {};
    const allKeys = new Set([
        ...Object.keys(defaultConfig),
        ...Object.keys(serverVisualConfig),
        ...Object.keys(queryVisualConfig)
    ]);
    for (const key of allKeys) {
        const defaultVal = defaultConfig[key];
        const evalDefaultVal = typeof defaultVal === "function" ? defaultVal(result) : defaultVal;
        const serverVal = serverVisualConfig[key];
        const queryVal = queryVisualConfig[key];
        if (evalDefaultVal && typeof evalDefaultVal === "object") {
            result[key] = merge(evalDefaultVal, (typeof serverVal == "object") ? serverVal : {}, (typeof queryVal == "object") ? queryVal : {});
        } else {
            result[key] = queryVal ?? serverVal ?? evalDefaultVal;
        }
    }
    return result as T;
}

function expandDots(config, result = {}) {
    for (const key in config) {
        const value = config[key];
        const parts = key.split(".");
        let current = result;
        for (let i = 0; i < parts.length - 1; i++) {
            if (!(parts[i] in current)) {
                current[parts[i]] = {};
            }
            current = current[parts[i]];
        }
        if (typeof value === "object") {
            current[parts[parts.length - 1]] = expandDots(value, current[parts[parts.length - 1]]);
        } else {
            current[parts[parts.length - 1]] = value;
        }
    }
    return result;
}

const config: Readonly<OverlayConfig> = merge<OverlayConfig>(defaultConfig, expandDots(visualConfig), expandDots(queryVisualConfig));

setFavicon(faviconTemplate
    .replaceAll("{CONTEST_COLOR}", config.CONTEST_COLOR)
    .replaceAll("{TEXT_COLOR}", isShouldUseDarkColor(config.CONTEST_COLOR) ? "#000000" : "#FFFFFF"));

export default config;
