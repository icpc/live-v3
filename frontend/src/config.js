// Strings
export const BASE_URL_WS = process.env.REACT_APP_WEBSOCKET_URL ?? "ws://localhost:8080/overlay";

// Non Styling configs
export const WEBSOCKET_RECONNECT_TIME = 5000; // ms

export const SCOREBOARD_TEAMS_ON_PAGE = 23;
export const SCOREBOARD_MAX_PAGES = Infinity;


// Timings
export const WIDGET_TRANSITION_TIME = 300; // ms
export const QUEUE_ROW_TRANSITION_TIME = 1000; // ms
export const QUEUE_ROW_APPEAR_TIME = QUEUE_ROW_TRANSITION_TIME; // ms
export const QUEUE_ROW_FTS_TRANSITION_TIME = 3000; // ms
export const SCOREBOARD_ROW_TRANSITION_TIME = 1000; // ms
export const SCOREBOARD_SCROLL_INTERVAL = 2000; // ms;
export const PICTURES_APPEAR_TIME = 1000; // ms
export const TICKER_SCROLL_TRANSITION_TIME = 1000; //ms

// Styles
export const VERDICT_OK = "#1b8041";
export const VERDICT_NOK = "#881f1b";
export const VERDICT_UNKNOWN = "#a59e0c";


export const QUEUE_ROW_HEIGHT = 41; // px
export const QUEUE_FTS_PADDING = QUEUE_ROW_HEIGHT / 2; // px
export const QUEUE_ROWS_COUNT = 15; // n
export const QUEUE_OPACITY = 0.8;

export const SCOREBOARD_RANK_WIDTH = 80; // px
export const SCOREBOARD_NAME_WIDTH = 300; // px
export const SCOREBOARD_SUM_PEN_WIDTH = 80; // px
export const SCOREBOARD_HEADER_TITLE_BG_COLOR = VERDICT_NOK;
export const SCOREBOARD_HEADER_TITLE_FONT_SIZE = "30px";
export const SCOREBOARD_HEADER_BG_COLOR = "#000000";
export const SCOREBOARD_OPACITY = 0.8;

export const STATISTICS_TITLE_FONT_SIZE = "30px";
export const STATISTICS_OPACITY = 0.8;
export const STATISTICS_BG_COLOR = "#000000";
export const STATISTICS_TITLE_COLOR = "#FFFFFF";
export const STATISTICS_STATS_VALUE_FONT_SIZE = "24pt";
export const STATISTICS_STATS_VALUE_FONT_FAMILY = "Helvetica, serif";
export const STATISTICS_STATS_VALUE_COLOR = "#FFFFFF";


export const CELL_FONT_FAMILY = "Helvetica, serif";
export const CELL_FONT_SIZE = "22pt";
export const CELL_TEXT_COLOR = "#FFFFFF";
export const CELL_BG_COLOR = "#000000";
export const CELL_BG_COLOR_ODD = "rgba(1, 1, 1, 0.9)";

export const CELL_PROBLEM_LINE_WIDTH = "5px"; // css property
export const CELL_QUEUE_VERDICT_WIDTH = "50px"; // css property
export const CELL_QUEUE_RANK_WIDTH = "50px"; // css property
export const CELL_QUEUE_TOTAL_SCORE_WIDTH = "50px"; // css property
export const CELL_QUEUE_TASK_WIDTH = "50px"; // css property

export const CELL_NAME_LEFT_PADDING = "5px"; // css property
export const CELL_NAME_RIGHT_PADDING = CELL_NAME_LEFT_PADDING; // css property
export const CELL_NAME_FONT = CELL_FONT_SIZE + " " + CELL_FONT_FAMILY;

export const TICKER_SMALL_SIZE = "10%"; // css property
export const TICKER_SMALL_BACKGROUND = VERDICT_NOK;
export const TICKER_BACKGROUND = CELL_BG_COLOR;
export const TICKER_OPACITY = 0.8;
export const TICKER_FONT_COLOR = "#FFFFFF";
export const TICKER_FONT_FAMILY = "Helvetica, serif";
export const TICKER_TEXT_FONT_SIZE = "34px"; // css property
export const TICKER_TEXT_MARGIN_LEFT = "7px"; // css property
export const TICKER_CLOCK_FONT_SIZE = "34px"; // css property
export const TICKER_CLOCK_MARGIN_LEFT = "7px"; // css property


export const STAR_SIZE = 10; // px


// Medals
export const getMedalColor = (rank) => {
    switch (true) {
    case (rank < 5):
        return "gold";
    case (rank < 10):
        return "silver";
    case (rank < 15):
        return "#7f4c19";
    default:
        return undefined;
    }
};

// Debug Behaviour
export const LOG_LINES = 300;
