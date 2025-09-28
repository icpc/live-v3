import { LocationRectangle } from "@/utils/location-rectangle";

// Explicit interface for the evaluated overlay config (after applying defaults and lambdas)
export interface OverlayConfig {
  // Base / connectivity
  CONTEST_COLOR: string;
  CONTEST_CAPTION: string;
  BASE_URL_WS: string;

  // Non-styling configs
  WEBSOCKET_RECONNECT_TIME: number; // ms

  // Strings
  QUEUE_TITLE: string;
  QUEUE_CAPTION: string;
  SCOREBOARD_CAPTION: string;
  STATISTICS_TITLE: string;
  STATISTICS_CAPTION: string;

  // Behaviour
  TICKER_SCOREBOARD_REPEATS: number;
  QUEUE_MAX_ROWS: number;
  QUEUE_HORIZONTAL_HEIGHT_NUM: number;

  // Timings
  WIDGET_TRANSITION_TIME: number; // ms
  QUEUE_ROW_TRANSITION_TIME: number; // ms
  QUEUE_ROW_APPEAR_TIME: number; // ms
  QUEUE_ROW_FEATURED_RUN_APPEAR_TIME: number; // ms
  QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY: number; // ms
  QUEUE_ROW_FTS_TRANSITION_TIME: number; // ms
  SCOREBOARD_ROW_TRANSITION_TIME: number; // ms
  SCOREBOARD_SCROLL_INTERVAL: number; // ms
  PICTURES_APPEAR_TIME: number; // ms
  SVG_APPEAR_TIME: number; // ms
  VIDEO_APPEAR_TIME: number; // ms
  TEAM_VIEW_APPEAR_TIME: number; // ms
  PVP_APPEAR_TIME: number; // ms
  TICKER_SCROLL_TRANSITION_TIME: number; // ms
  TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME: number; // ms
  STATISTICS_CELL_MORPH_TIME: number; // ms
  CELL_FLASH_PERIOD: number; // ms

  // Styles > Global
  GLOBAL_DEFAULT_FONT_FAMILY: string; // css-property
  GLOBAL_DEFAULT_FONT_SIZE: string; // css-property
  GLOBAL_DEFAULT_FONT_WEIGHT: number; // css-property
  GLOBAL_DEFAULT_FONT_WEIGHT_BOLD: number; // css-property
  GLOBAL_DEFAULT_FONT: string; // MUST HAVE FONT SIZE
  GLOBAL_BACKGROUND_COLOR: string;
  GLOBAL_TEXT_COLOR: string;
  GLOBAL_BORDER_RADIUS: string;

  // Extra CSS configurations
  EXTRA_CSS: string | null; // Additional CSS that can be injected

  VERDICT_OK: string;
  VERDICT_NOK: string;
  VERDICT_UNKNOWN: string;

  // Styles > Scoreboard
  SCOREBOARD_BACKGROUND_COLOR: string;
  SCOREBOARD_BORDER_RADIUS: string;
  SCOREBOARD_TEXT_COLOR: string;
  SCOREBOARD_CAPTION_FONT_SIZE: string; // css value
  SCOREBOARD_HEADER_BACKGROUND_COLOR: string;
  SCOREBOARD_HEADER_DIVIDER_COLOR: string;
  SCOREBOARD_HEADER_FONT_SIZE: string;
  SCOREBOARD_HEADER_FONT_WEIGHT: number;
  SCOREBOARD_HEADER_HEIGHT: number;
  SCOREBOARD_ROWS_DIVIDER_COLOR: string;
  SCOREBOARD_ROW_HEIGHT: number; // px
  SCOREBOARD_ROW_PADDING: number; // px
  SCOREBOARD_BETWEEN_HEADER_PADDING: number; // px
  SCOREBOARD_ROW_FONT_SIZE: string;
  SCOREBOARD_TABLE_ROW_FONT_WEIGHT: number

  SCOREBOARD_CELL_PLACE_SIZE: string;
  SCOREBOARD_CELL_TEAMNAME_SIZE: string;
  SCOREBOARD_CELL_TEAMNANE_ALIGN: string;
  SCOREBOARD_CELL_POINTS_SIZE: string;
  SCOREBOARD_CELL_POINTS_ALIGN: string;
  SCOREBOARD_CELL_PENALTY_SIZE: string;
  SCOREBOARD_CELL_PENALTY_ALIGN: string;

  SCOREBOARD_NORMAL_NAME: string;
  SCOREBOARD_OPTIMISTIC_NAME: string;
  SCOREBOARD_PESSIMISTIC_NAME: string;
  SCOREBOARD_UNDEFINED_NAME: string;
  SCOREBOARD_STANDINGS_NAME: string;

  // Queue
  QUEUE_ROW_FONT_SIZE: string;
  QUEUE_ROW_BACKGROUND: string;
  QUEUE_ROW_HEIGHT: number; // px
  QUEUE_ROW_WIDTH: number; // px
  QUEUE_ROW_Y_PADDING: number; // px
  QUEUE_HORIZONTAL_ROW_Y_PADDING: number; // px
  QUEUE_ROW_FEATURED_RUN_PADDING: number; // px
  QUEUE_WRAP_PADDING: number; // px
  QUEUE_HORIZONTAL_ROW_X_PADDING: number; // px
  QUEUE_OPACITY: number;
  QUEUE_FEATURED_RUN_ASPECT: number;
  QUEUE_BACKGROUND_COLOR: string;
  QUEUE_HORIZONTAL_BACKGROUND_COLOR: string;
  QUEUE_ROW_PROBLEM_LABEL_WIDTH: number; // px
  QUEUE_HEADER_FONT_SIZE: string;
  QUEUE_HEADER_LINE_HEIGHT: string;

  SCORE_NONE_TEXT: string;

  // Statistics
  STATISTICS_TITLE_FONT_SIZE: string;
  STATISTICS_OPACITY: number;
  STATISTICS_BG_COLOR: string;
  STATISTICS_TITLE_COLOR: string;
  STATISTICS_STATS_VALUE_FONT_SIZE: string;
  STATISTICS_STATS_VALUE_FONT_FAMILY: string;
  STATISTICS_STATS_VALUE_COLOR: string;
  STATISTICS_BAR_HEIGHT_PX: number;
  STATISTICS_BAR_HEIGHT: string;
  STATISTICS_BAR_GAP_PX: number;
  STATISTICS_BAR_GAP: string;

  // Cells (legacy TODO: remove)
  CELL_FONT_FAMILY: string;
  CELL_FONT_SIZE: string;
  CELL_TEXT_COLOR: string;
  CELL_TEXT_COLOR_INVERSE: string;
  CELL_BG_COLOR: string;
  CELL_BG_COLOR_ODD: string;
  CELL_BG_COLOR2: string;
  CELL_BG_COLOR_ODD2: string;

  CELL_PROBLEM_LINE_WIDTH: string; // css property
  CELL_QUEUE_VERDICT_WIDTH: string; // css property
  CELL_QUEUE_VERDICT_WIDTH2: string; // css property
  CELL_QUEUE_RANK_WIDTH: string; // css property
  CELL_QUEUE_RANK_WIDTH2: string; // css property
  CELL_QUEUE_TOTAL_SCORE_WIDTH: string; // css property
  CELL_QUEUE_TASK_WIDTH: string; // css property

  CELL_NAME_LEFT_PADDING: string; // css property
  CELL_NAME_RIGHT_PADDING: string; // css property

  // Ticker
  TICKER_SMALL_SIZE: string; // css property
  TICKER_SMALL_BACKGROUND: string;
  TICKER_BACKGROUND: string;
  TICKER_DEFAULT_SCOREBOARD_BACKGROUND: string;
  TICKER_OPACITY: number;
  TICKER_FONT_COLOR: string;
  TICKER_FONT_FAMILY: string;
  TICKER_TEXT_FONT_SIZE: string; // css property
  TICKER_TEXT_MARGIN_LEFT: string; // css property
  TICKER_CLOCK_FONT_SIZE: string; // css property
  TICKER_CLOCK_MARGIN_LEFT: string; // css property
  TICKER_SCOREBOARD_RANK_WIDTH: string; // css property
  TICKER_LIVE_ICON_SIZE: string;

  // Picture name
  PICTURE_NAME_BACKGROUND_COLOR: string;
  PICTURE_NAME_FONT_COLOR: string;
  PICTURE_NAME_FONT_SIZE: string;
  PICTURE_BORDER_SIZE: string;
  PICTURE_NAME_FONT_FAMILY: string;

  // Misc
  TEAMVIEW_SMALL_FACTOR: string; // css property

  FULL_SCREEN_CLOCK_CENTERED: boolean;
  FULL_SCREEN_CLOCK_PADDING_TOP: string;
  FULL_SCREEN_CLOCK_FONT_SIZE: string;
  FULL_SCREEN_CLOCK_COLOR: string;
  FULL_SCREEN_CLOCK_FONT_FAMILY: string;

  ADVERTISEMENT_BACKGROUND: string; // hex value.
  ADVERTISEMENT_COLOR: string;
  ADVERTISEMENT_FONT_FAMILY: string;

  STAR_SIZE: number; // px

  QUEUE_PROBLEM_LABEL_FONT_SIZE: string;

  // Medals
  MEDAL_COLORS: {
    gold: string;
    silver: string;
    bronze: string;
  };

  // Debug Behaviour
  LOG_LINES: number;

  CONTESTER_ROW_OPACITY: number;

  CONTESTER_FONT_SIZE: string;
  CONTESTER_BACKGROUND_COLOR: string;

  CONTESTER_ROW_BORDER_RADIUS: string;
  CONTESTER_ROW_HEIGHT: string;
  CONTESTER_NAME_WIDTH: string;
  CONTESTER_INFO_SCORE_WIDTH: string;
  CONTESTER_INFO_RANK_WIDTH: string;

  // Timeline
  TIMELINE_ELEMENT_DIAMETER: number;
  TIMELINE_BORDER_RADIUS: string;
  TIMELINE_LINE_HEIGHT: number;
  TIMELINE_WRAP_HEIGHT: number;
  TIMELINE_TIMEBORDER_COLOR: string;
  TIMELINE_REAL_WIDTH: number;
  TIMELINE_PADDING: number;
  TIMELINE_LEFT_TIME_PADDING: number;
  TIMELINE_ANIMATION_TIME: number; // ms

  TIMELINE_WRAP_HEIGHT_PVP: number;
  TIMELINE_ELEMENT_DIAMETER_PVP: number;
  TIMELINE_PADDING_PVP: number;
  TIMELINE_REAL_WIDTH_PVP: number;

  // Keylog
  KEYLOG_MAXIMUM_FOR_NORMALIZATION: number; // max value for normalization
  KEYLOG_TOP_PADDING: number; // px
  KEYLOG_BOTTOM_PADDING: number; // px
  KEYLOG_Z_INDEX: number;

  KEYLOG_ANIMATION_DURATION: number; // ms
  KEYLOG_ANIMATION_EASING: string;

  KEYLOG_STROKE_WIDTH: number; // px

  KEYLOG_STROKE_DARK: string;
  KEYLOG_STROKE_LIGHT: string;
  KEYLOG_FILL_DARK: string;
  KEYLOG_FILL_LIGHT: string;
  KEYLOG_GLOW_BLUR: number;

  // Layout factors
  TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR: number;
  SPLITSCREEN_SECONDARY_FACTOR: number;

  // PVP / Teamview
  PVP_BACKGROUND: string;
  PVP_TABLE_ROW_HEIGHT: number;
  PVP_TEAM_STATUS_TASK_WIDTH: number;

  CIRCLE_PROBLEM_SIZE: string;
  CIRCLE_PROBLEM_LINE_WIDTH: string;

  CELL_INFO_VERDICT_WIDTH: string; // css property

  // layers (z-indexes)
  QUEUE_BASIC_ZINDEX: number;

  LOCATOR_MAGIC_CONSTANT: number;

  // Computed websocket urls
  WEBSOCKETS: {
    mainScreen: string;
    contestInfo: string;
    queue: string;
    statistics: string;
    ticker: string;
    scoreboardNormal: string;
    scoreboardOptimistic: string;
    scoreboardPessimistic: string;
  };

  // Widget positions
  WIDGET_POSITIONS: {
    advertisement: LocationRectangle;
    picture: LocationRectangle;
    svg: LocationRectangle;
    queue: LocationRectangle;
    scoreboard: LocationRectangle;
    statistics: LocationRectangle;
    ticker: LocationRectangle;
    fullScreenClock: LocationRectangle;
    teamLocator: LocationRectangle;
    teamview: {
      SINGLE: LocationRectangle;
      PVP_TOP: LocationRectangle;
      PVP_BOTTOM: LocationRectangle;
      TOP_LEFT: LocationRectangle;
      TOP_RIGHT: LocationRectangle;
      BOTTOM_LEFT: LocationRectangle;
      BOTTOM_RIGHT: LocationRectangle;
    };
  };
}
