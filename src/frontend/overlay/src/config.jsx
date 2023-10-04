// Strings
const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
const WS_PORT = import.meta.env.VITE_WEBSOCKET_PORT ?? window.location.port;
export const BASE_URL_WS = import.meta.env.VITE_WEBSOCKET_URL ?? WS_PROTO + window.location.hostname + ":" + WS_PORT + "/api/overlay";
export const VISUAL_CONFIG_URL = import.meta.env.VITE_VISUAL_CONFIG_URL ?? `${window.location.protocol}//${window.location.hostname}:${WS_PORT}/api/overlay/visualConfig.json`;

const visualConfig = await fetch(VISUAL_CONFIG_URL)
    .then(r => r.json())
    .catch((e) => console.error("failed to load visual config: " + e)) ?? {};

// to generate next consts use replace `export const ([\w]*) = ([^visual])` to `export const $1 = visualConfig\["$1"\] ?? $2`

const CONTEST_COLOR = visualConfig["CONTEST_COLOR"] ?? "#4C83C3";

// Non Styling configs
export const WEBSOCKET_RECONNECT_TIME = visualConfig["WEBSOCKET_RECONNECT_TIME"] ?? 5000; // ms
export const QUEUE_TITLE = visualConfig["QUEUE_TITLE"] ?? "Queue";
export const QUEUE_CAPTION = visualConfig[""] ?? "46th";

export const SCOREBOARD_CAPTION = visualConfig["SCOREBOARD_CAPTION"] ?? "46th";
export const SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR = visualConfig["SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR"] ?? CONTEST_COLOR;
export const SCOREBOARD_TABLE_HEADER_DIVIDER_COLOR = visualConfig["SCOREBOARD_TABLE_HEADER_DIVIDER_COLOR"] ?? "#242425"
export const SCOREBOARD_TABLE_ROWS_DIVIDER_COLOR = visualConfig["SCOREBOARD_TABLE_ROWS_DIVIDER_COLOR"] ?? CONTEST_COLOR;

// Behaviour
export const TICKER_SCOREBOARD_REPEATS = visualConfig["TICKER_SCOREBOARD_REPEATS"] ?? 1;
export const QUEUE_MAX_ROWS = visualConfig["QUEUE_MAX_ROWS"] ?? 12;

// Timings
export const WIDGET_TRANSITION_TIME = visualConfig["WIDGET_TRANSITION_TIME"] ?? 300; // ms
export const QUEUE_ROW_TRANSITION_TIME = visualConfig["QUEUE_ROW_TRANSITION_TIME"] ?? 1000; // ms
export const QUEUE_ROW_APPEAR_TIME = visualConfig["QUEUE_ROW_APPEAR_TIME"] ?? QUEUE_ROW_TRANSITION_TIME; // ms
export const QUEUE_ROW_FEATURED_RUN_APPEAR_TIME = visualConfig["QUEUE_ROW_FEATURED_RUN_APPEAR_TIME"] ?? 500; // ms
export const QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY = visualConfig["QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY"] ?? 5000; // ms
export const QUEUE_ROW_FTS_TRANSITION_TIME = visualConfig["QUEUE_ROW_FTS_TRANSITION_TIME"] ?? 3000; // ms
export const SCOREBOARD_ROW_TRANSITION_TIME = visualConfig["SCOREBOARD_ROW_TRANSITION_TIME"] ?? 1000; // ms
export const SCOREBOARD_SCROLL_INTERVAL = visualConfig["SCOREBOARD_SCROLL_INTERVAL"] ?? 20000; // ms
export const PICTURES_APPEAR_TIME = visualConfig["PICTURES_APPEAR_TIME"] ?? 1000; // ms
export const SVG_APPEAR_TIME = visualConfig["SVG_APPEAR_TIME"] ?? 1000; // ms
export const VIDEO_APPEAR_TIME = visualConfig["VIDEO_APPEAR_TIME"] ?? 100; // ms
export const TEAM_VIEW_APPEAR_TIME = visualConfig["TEAM_VIEW_APPEAR_TIME"] ?? 1000; // ms
export const PVP_APPEAR_TIME = visualConfig["PVP_APPEAR_TIME"] ?? 1000; // ms
export const TICKER_SCROLL_TRANSITION_TIME = visualConfig["TICKER_SCROLL_TRANSITION_TIME"] ?? 1000; //ms
export const TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME = visualConfig["TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME"] ?? 300; //ms
export const STATISTICS_CELL_MORPH_TIME = visualConfig["STATISTICS_CELL_MORPH_TIME"] ?? 200; //ms
export const CELL_FLASH_PERIOD = visualConfig["CELL_FLASH_PERIOD"] ?? 500; //ms
// Behaviour

// Styles

export const GLOBAL_DEFAULT_FONT_FAMILY = visualConfig["GLOBAL_DEFAULT_FONT_FAMILY"] ?? "Helvetica, serif"; // css-property
export const GLOBAL_DEFAULT_FONT_SIZE = visualConfig["GLOBAL_DEFAULT_FONT_SIZE"] ?? "18px"; // css-property
export const GLOBAL_DEFAULT_FONT = visualConfig["GLOBAL_DEFAULT_FONT"] ?? GLOBAL_DEFAULT_FONT_SIZE + " " + GLOBAL_DEFAULT_FONT_FAMILY; // css property MUST HAVE FONT SIZE

export const VERDICT_OK = visualConfig["VERDICT_OK"] ?? "#1b8041";
export const VERDICT_NOK = visualConfig["VERDICT_NOK"] ?? "#881f1b";
export const VERDICT_UNKNOWN = visualConfig["VERDICT_UNKNOWN"] ?? "#a59e0c";
export const VERDICT_OK2 = visualConfig["VERDICT_OK2"] ?? "#3bba6b";
export const VERDICT_NOK2 = visualConfig["VERDICT_NOK2"] ?? "#CB2E28";
export const VERDICT_UNKNOWN2 = visualConfig["VERDICT_UNKNOWN2"] ?? "#F3BE4B";


export const QUEUE_ROW_HEIGHT = visualConfig["QUEUE_ROW_HEIGHT"] ?? 41; // px
export const QUEUE_ROW_HEIGHT2 = visualConfig["QUEUE_ROW_HEIGHT2"] ?? 25; // px
export const QUEUE_FTS_PADDING = visualConfig["QUEUE_FTS_PADDING"] ?? QUEUE_ROW_HEIGHT / 2; // px
export const QUEUE_OPACITY = visualConfig["QUEUE_OPACITY"] ?? 0.95;
export const QUEUE_FEATURED_RUN_ASPECT = visualConfig["QUEUE_FEATURED_RUN_ASPECT"] ?? 16 / 9;
export const QUEUE_BACKGROUND_COLOR = visualConfig["QUEUE_BACKGROUND_COLOR"] ?? CONTEST_COLOR;

export const SCOREBOARD_RANK_WIDTH = visualConfig["SCOREBOARD_RANK_WIDTH"] ?? "80px"; // px
export const SCOREBOARD_RANK_WIDTH2 = visualConfig["SCOREBOARD_RANK_WIDTH2"] ?? "50px"; // px
export const SCOREBOARD_NAME_WIDTH = visualConfig["SCOREBOARD_NAME_WIDTH"] ?? "300px"; // px
export const SCOREBOARD_NAME_WIDTH2 = visualConfig["SCOREBOARD_NAME_WIDTH2"] ?? "350px"; // px
export const SCOREBOARD_SUM_PEN_WIDTH = visualConfig["SCOREBOARD_SUM_PEN_WIDTH"] ?? "80px"; // px
export const SCOREBOARD_HEADER_TITLE_BG_COLOR = visualConfig["SCOREBOARD_HEADER_TITLE_BG_COLOR"] ?? VERDICT_NOK;
export const SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR = visualConfig["SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR"] ?? VERDICT_OK;
export const SCOREBOARD_HEADER_TITLE_FONT_SIZE = visualConfig["SCOREBOARD_HEADER_TITLE_FONT_SIZE"] ?? "30px";
export const SCOREBOARD_HEADER_BG_COLOR = visualConfig["SCOREBOARD_HEADER_BG_COLOR"] ?? "#000000";
export const SCOREBOARD_OPACITY = visualConfig["SCOREBOARD_OPACITY"] ?? 0.95;

export const SCORE_NONE_TEXT = visualConfig["SCORE_NONE_TEXT"] ?? ".";

// export const PVP_OPACITY = 0.95;
// export const TEAM_VIEW_OPACITY = 0.95;

export const STATISTICS_TITLE_FONT_SIZE = visualConfig["STATISTICS_TITLE_FONT_SIZE"] ?? "30px";
export const STATISTICS_OPACITY = visualConfig["STATISTICS_OPACITY"] ?? 0.95;
export const STATISTICS_BG_COLOR = visualConfig["STATISTICS_BG_COLOR"] ?? "#000000";
export const STATISTICS_TITLE_COLOR = visualConfig["STATISTICS_TITLE_COLOR"] ?? "#FFFFFF";
export const STATISTICS_STATS_VALUE_FONT_SIZE = visualConfig["STATISTICS_STATS_VALUE_FONT_SIZE"] ?? "24pt";
export const STATISTICS_STATS_VALUE_FONT_FAMILY = visualConfig["STATISTICS_STATS_VALUE_FONT_FAMILY"] ?? GLOBAL_DEFAULT_FONT_FAMILY;
export const STATISTICS_STATS_VALUE_COLOR = visualConfig["STATISTICS_STATS_VALUE_COLOR"] ?? "#FFFFFF";


export const CELL_FONT_FAMILY = visualConfig["CELL_FONT_FAMILY"] ?? GLOBAL_DEFAULT_FONT_FAMILY;
export const CELL_FONT_SIZE = visualConfig["CELL_FONT_SIZE"] ?? "18px";
export const CELL_TEXT_COLOR = visualConfig["CELL_TEXT_COLOR"] ?? "#FFFFFF";
export const CELL_TEXT_COLOR_INVERSE = visualConfig["CELL_TEXT_COLOR_INVERSE"] ?? "#000000";
export const CELL_BG_COLOR = visualConfig["CELL_BG_COLOR"] ?? "#000000";
export const CELL_BG_COLOR_ODD = visualConfig["CELL_BG_COLOR_ODD"] ?? "rgba(1, 1, 1, 0.9)";
export const CELL_BG_COLOR2 = visualConfig["CELL_BG_COLOR2"] ?? "#1E1E1E";
export const CELL_BG_COLOR_ODD2 = visualConfig["CELL_BG_COLOR_ODD2"] ?? "#242424";

export const CELL_PROBLEM_LINE_WIDTH = visualConfig["CELL_PROBLEM_LINE_WIDTH"] ?? "5px"; // css property
export const CELL_QUEUE_VERDICT_WIDTH = visualConfig["CELL_QUEUE_VERDICT_WIDTH"] ?? "80px"; // css property
export const CELL_QUEUE_VERDICT_WIDTH2 = visualConfig["CELL_QUEUE_VERDICT_WIDTH2"] ?? "20px"; // css property
export const CELL_QUEUE_RANK_WIDTH = visualConfig["CELL_QUEUE_RANK_WIDTH"] ?? "50px"; // css property
export const CELL_QUEUE_RANK_WIDTH2 = visualConfig["CELL_QUEUE_RANK_WIDTH2"] ?? "30px"; // css property
export const CELL_QUEUE_TOTAL_SCORE_WIDTH = visualConfig["CELL_QUEUE_TOTAL_SCORE_WIDTH"] ?? "50px"; // css property
export const CELL_QUEUE_TASK_WIDTH = visualConfig["CELL_QUEUE_TASK_WIDTH"] ?? "50px"; // css property

export const CELL_NAME_LEFT_PADDING = visualConfig["CELL_NAME_LEFT_PADDING"] ?? "5px"; // css property
export const CELL_NAME_RIGHT_PADDING = visualConfig["CELL_NAME_RIGHT_PADDING"] ?? CELL_NAME_LEFT_PADDING; // css property

export const TICKER_SMALL_SIZE = visualConfig["TICKER_SMALL_SIZE"] ?? "12%"; // css property
export const TICKER_SMALL_BACKGROUND = VERDICT_NOK;
export const TICKER_BACKGROUND = visualConfig["TICKER_BACKGROUND"] ?? CELL_BG_COLOR;
export const TICKER_OPACITY = visualConfig["TICKER_OPACITY"] ?? 0.95;
export const TICKER_FONT_COLOR = visualConfig["TICKER_FONT_COLOR"] ?? "#FFFFFF";
export const TICKER_FONT_FAMILY = visualConfig["TICKER_FONT_FAMILY"] ?? "Helvetica, serif";
export const TICKER_TEXT_FONT_SIZE = visualConfig["TICKER_TEXT_FONT_SIZE"] ?? "32px"; // css property
export const TICKER_TEXT_MARGIN_LEFT = visualConfig["TICKER_TEXT_MARGIN_LEFT"] ?? "16px"; // css property
export const TICKER_CLOCK_FONT_SIZE = visualConfig["TICKER_CLOCK_FONT_SIZE"] ?? "32px"; // css property
export const TICKER_CLOCK_MARGIN_LEFT = visualConfig["TICKER_CLOCK_MARGIN_LEFT"] ?? "10px"; // css property
export const TICKER_SCOREBOARD_RANK_WIDTH = visualConfig["TICKER_SCOREBOARD_RANK_WIDTH"] ?? "50px"; // css property
export const TICKER_LIVE_ICON_SIZE = visualConfig["TICKER_LIVE_ICON_SIZE"] ?? "32px";


export const TEAMVIEW_SMALL_FACTOR = visualConfig["TEAMVIEW_SMALL_FACTOR"] ?? "50%"; // css property

export const FULL_SCREEN_CLOCK_FONT_SIZE = visualConfig["FULL_SCREEN_CLOCK_FONT_SIZE"] ?? "400px";
export const FULL_SCREEN_CLOCK_COLOR = visualConfig["FULL_SCREEN_CLOCK_COLOR"] ?? "#eeeeee";
export const FULL_SCREEN_CLOCK_FONT_FAMILY = visualConfig["FULL_SCREEN_CLOCK_FONT_FAMILY"] ?? "Helvetica, monospace";

export const STAR_SIZE = visualConfig["STAR_SIZE"] ?? 20; // px

export const QUEUE_PROBLEM_LABEL_FONT_SIZE = visualConfig["QUEUE_PROBLEM_LABEL_FONT_SIZE"] ?? "14px";

// Medals
export const MEDAL_COLORS = visualConfig["MEDAL_COLORS"] ?? Object.freeze({
    "gold": "#F9A80D",
    "silver": "#ACACAC",
    "bronze": "#E27B5A"
});

// Debug Behaviour
export const LOG_LINES = visualConfig["LOG_LINES"] ?? 300;

export const GLOBAL_BACKGROUND_COLOR = visualConfig["GLOBAL_BACKGROUND_COLOR"] ?? "#242425";

export const SCOREBOARD_BACKGROUND_COLOR = visualConfig["SCOREBOARD_BACKGROUND_COLOR"] ?? GLOBAL_BACKGROUND_COLOR;

export const CONTESTER_ROW_OPACITY = visualConfig["CONTESTER_ROW_OPACITY"] ?? 0.95;
export const CONTESTER_BACKGROUND_COLOR = visualConfig["CONTESTER_BACKGROUND_COLOR"] ?? "#4C83C3";

export const CONTESTER_ROW_BORDER_RADIUS = visualConfig["CONTESTER_ROW_BORDER_RADIUS"] ?? "16px";
export const CONTESTER_ROW_HEIGHT = visualConfig["CONTESTER_ROW_HEIGHT"] ?? "25px";
export const CONTESTER_NAME_WIDTH = visualConfig["CONTESTER_NAME_WIDTH"] ?? "150px";
export const CONTESTER_ROW_VERDICT_FONT_SIZE2 = visualConfig["CONTESTER_ROW_VERDICT_FONT_SIZE2"] ?? "16px"; // css-property

export const QUEUE_PER_COLUMNS_PADDING2 = visualConfig["QUEUE_PER_COLUMNS_PADDING2"] ?? "5px"; // css property
export const QUEUE_VERDICT_PADDING_LEFT2 = visualConfig["QUEUE_VERDICT_PADDING_LEFT2"] ?? "6px"; // css property
export const CIRCLE_PROBLEM_SIZE = visualConfig["CIRCLE_PROBLEM_SIZE"] ?? "28px";
export const GLOBAL_BORDER_RADIUS = visualConfig["GLOBAL_BORDER_RADIUS"] ?? "16px";
export const CIRCLE_PROBLEM_LINE_WIDTH = visualConfig["CIRCLE_PROBLEM_LINE_WIDTH"] ?? "3.5px";

export const CELL_INFO_VERDICT_WIDTH = visualConfig["CELL_INFO_VERDICT_WIDTH"] ?? "100px"; // css property

// layers (z-indexes)
export const QUEUE_BASIC_ZINDEX = visualConfig["QUEUE_BASIC_ZINDEX"] ?? 20;

