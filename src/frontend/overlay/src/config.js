// Strings
const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
const WS_PORT = process.env.REACT_APP_WEBSOCKET_PORT ?? window.location.port;
export const BASE_URL_WS = process.env.REACT_APP_WEBSOCKET_URL ?? WS_PROTO + window.location.hostname + ":" + WS_PORT + "/api/overlay";

// Non Styling configs
export const WEBSOCKET_RECONNECT_TIME = 5000; // ms

// Behaviour
export const TICKER_SCOREBOARD_REPEATS = 1;

// Timings
export const WIDGET_TRANSITION_TIME = 300; // ms
export const QUEUE_ROW_TRANSITION_TIME = 1000; // ms
export const QUEUE_ROW_APPEAR_TIME = QUEUE_ROW_TRANSITION_TIME; // ms
export const QUEUE_ROW_FEATURED_RUN_APPEAR_TIME = 500; // ms
export const QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY = 5000; // ms
export const QUEUE_ROW_FTS_TRANSITION_TIME = 3000; // ms
export const SCOREBOARD_ROW_TRANSITION_TIME = 1000; // ms
export const SCOREBOARD_SCROLL_INTERVAL = 20000; // ms
export const PICTURES_APPEAR_TIME = 1000; // ms
export const SVG_APPEAR_TIME = 1000; // ms
export const VIDEO_APPEAR_TIME = 100; // ms
export const TEAM_VIEW_APPEAR_TIME = 1000; // ms
export const PVP_APPEAR_TIME = 1000; // ms
export const TICKER_SCROLL_TRANSITION_TIME = 1000; //ms
export const TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME = 300; //ms
export const STATISTICS_CELL_MORPH_TIME = 200; //ms
export const CELL_FLASH_PERIOD = 500; //ms
// Behaviour

// Styles

export const GLOBAL_DEFAULT_FONT_FAMILY = "Helvetica, serif"; // css-property
export const GLOBAL_DEFAULT_FONT_SIZE = "22pt"; // css-property
export const GLOBAL_DEFAULT_FONT = GLOBAL_DEFAULT_FONT_SIZE + " " + GLOBAL_DEFAULT_FONT_FAMILY; // css property MUST HAVE FONT SIZE

export const VERDICT_OK = "#1b8041";
export const VERDICT_NOK = "#881f1b";
export const VERDICT_UNKNOWN = "#a59e0c";


export const QUEUE_ROW_HEIGHT = 41; // px
export const QUEUE_FTS_PADDING = QUEUE_ROW_HEIGHT / 2; // px
export const QUEUE_OPACITY = 0.95;
export const QUEUE_FEATURED_RUN_ASPECT = 16 / 9;

export const SCOREBOARD_RANK_WIDTH = "80px"; // px
export const SCOREBOARD_NAME_WIDTH = "300px"; // px
export const SCOREBOARD_SUM_PEN_WIDTH = "80px"; // px
export const SCOREBOARD_HEADER_TITLE_BG_COLOR = VERDICT_NOK;
export const SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR = VERDICT_OK;
export const SCOREBOARD_HEADER_TITLE_FONT_SIZE = "30px";
export const SCOREBOARD_HEADER_BG_COLOR = "#000000";
export const SCOREBOARD_OPACITY = 0.95;

export const SCORE_NONE_TEXT = ".";

// export const PVP_OPACITY = 0.95;
// export const TEAM_VIEW_OPACITY = 0.95;

export const STATISTICS_TITLE_FONT_SIZE = "30px";
export const STATISTICS_OPACITY = 0.95;
export const STATISTICS_BG_COLOR = "#000000";
export const STATISTICS_TITLE_COLOR = "#FFFFFF";
export const STATISTICS_STATS_VALUE_FONT_SIZE = "24pt";
export const STATISTICS_STATS_VALUE_FONT_FAMILY = GLOBAL_DEFAULT_FONT_FAMILY;
export const STATISTICS_STATS_VALUE_COLOR = "#FFFFFF";


export const CELL_FONT_FAMILY = GLOBAL_DEFAULT_FONT_FAMILY;
export const CELL_FONT_SIZE = "22pt";
export const CELL_TEXT_COLOR = "#FFFFFF";
export const CELL_BG_COLOR = "#000000";
export const CELL_BG_COLOR_ODD = "rgba(1, 1, 1, 0.9)";

export const CELL_PROBLEM_LINE_WIDTH = "5px"; // css property
export const CELL_QUEUE_VERDICT_WIDTH = "80px"; // css property
export const CELL_QUEUE_RANK_WIDTH = "50px"; // css property
export const CELL_QUEUE_TOTAL_SCORE_WIDTH = "50px"; // css property
export const CELL_QUEUE_TASK_WIDTH = "50px"; // css property

export const CELL_NAME_LEFT_PADDING = "5px"; // css property
export const CELL_NAME_RIGHT_PADDING = CELL_NAME_LEFT_PADDING; // css property

export const TICKER_SMALL_SIZE = "10%"; // css property
export const TICKER_SMALL_BACKGROUND = VERDICT_NOK;
export const TICKER_BACKGROUND = CELL_BG_COLOR;
export const TICKER_OPACITY = 0.95;
export const TICKER_FONT_COLOR = "#FFFFFF";
export const TICKER_FONT_FAMILY = "Helvetica, serif";
export const TICKER_TEXT_FONT_SIZE = "34px"; // css property
export const TICKER_TEXT_MARGIN_LEFT = "10px"; // css property
export const TICKER_CLOCK_FONT_SIZE = "34px"; // css property
export const TICKER_CLOCK_MARGIN_LEFT = "10px"; // css property
export const TICKER_SCOREBOARD_RANK_WIDTH = "50px"; // css property


export const TEAMVIEW_SMALL_FACTOR = "50%"; // css property

export const FULL_SCREEN_CLOCK_FONT_SIZE = "400px";
export const FULL_SCREEN_CLOCK_COLOR = "#eeeeee";
export const FULL_SCREEN_CLOCK_FONT_FAMILY = "Helvetica, monospace";

export const STAR_SIZE = 20; // px


// Medals
export const MEDAL_COLORS = Object.freeze({
    "gold": "#C2AC15",
    "silver": "#ABABAB",
    "bronze": "#7f4c19"
});

// Debug Behaviour
export const LOG_LINES = 300;
