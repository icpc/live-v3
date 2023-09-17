export interface ContestInfo {
  name: string;
  status: ContestStatus;
  resultType: ContestResultType;
  startTimeUnixMs: number;
  contestLengthMs: number;
  freezeTimeMs: number;
  problems: ProblemInfo[];
  teams: TeamInfo[];
  groups: GroupInfo[];
  organizations: OrganizationInfo[];
  penaltyRoundingMode: PenaltyRoundingMode;
  holdBeforeStartTimeMs?: number | null;
  emulationSpeed?: number;
  medals?: MedalType[];
  penaltyPerWrongAttempt?: string;
}

export enum ContestStatus {
  BEFORE = "BEFORE",
  RUNNING = "RUNNING",
  OVER = "OVER",
  FINALIZED = "FINALIZED",
}

export enum ContestResultType {
  ICPC = "ICPC",
  IOI = "IOI",
}

export enum PenaltyRoundingMode {
  each_submission_down_to_minute = "each_submission_down_to_minute",
  sum_down_to_minute = "sum_down_to_minute",
  sum_in_seconds = "sum_in_seconds",
  last = "last",
  zero = "zero",
}

export interface ProblemInfo {
  letter: string;
  name: string;
  id: number;
  ordinal: number;
  contestSystemId: string;
  minScore?: number | null;
  maxScore?: number | null;
  color?: string | null;
  scoreMergeMode?: ScoreMergeMode | null;
}

export interface TeamInfo {
  id: number;
  name: string;
  shortName: string;
  contestSystemId: string;
  groups: string[];
  hashTag: string | null;
  medias: { [key in TeamMediaType]: MediaType };
  isHidden: boolean;
  isOutOfContest: boolean;
  organizationId: string | null;
  customFields?: { [key: string]: string };
}

export interface GroupInfo {
  name: string;
  isHidden: boolean;
  isOutOfContest: boolean;
}

export interface OrganizationInfo {
  cdsId: string;
  displayName: string;
  fullName: string;
}

export interface MedalType {
  name: string;
  count: number;
  minScore?: number;
  tiebreakMode?: MedalTiebreakMode;
}

export enum ScoreMergeMode {
  MAX_PER_GROUP = "MAX_PER_GROUP",
  MAX_TOTAL = "MAX_TOTAL",
  LAST = "LAST",
  LAST_OK = "LAST_OK",
  SUM = "SUM",
}

export enum MedalTiebreakMode {
  NONE = "NONE",
  ALL = "ALL",
}

export enum TeamMediaType {
  camera = "camera",
  screen = "screen",
  record = "record",
  photo = "photo",
  reactionVideo = "reactionVideo",
  achievement = "achievement",
}

export type MediaType =
  | MediaType.Object
  | MediaType.Photo
  | MediaType.TaskStatus
  | MediaType.Video
  | MediaType.WebRTCGrabberConnection
  | MediaType.WebRTCProxyConnection;

export namespace MediaType {
  export enum Type {
    Object = "Object",
    Photo = "Photo",
    TaskStatus = "TaskStatus",
    Video = "Video",
    WebRTCGrabberConnection = "WebRTCGrabberConnection",
    WebRTCProxyConnection = "WebRTCProxyConnection",
  }
  
  export interface Object {
    type: MediaType.Type.Object;
    url: string;
    isMedia?: boolean;
  }
  
  export interface Photo {
    type: MediaType.Type.Photo;
    url: string;
    isMedia?: boolean;
  }
  
  export interface TaskStatus {
    type: MediaType.Type.TaskStatus;
    teamId: number;
    isMedia?: boolean;
  }
  
  export interface Video {
    type: MediaType.Type.Video;
    url: string;
    isMedia?: boolean;
  }
  
  export interface WebRTCGrabberConnection {
    type: MediaType.Type.WebRTCGrabberConnection;
    url: string;
    peerName: string;
    streamType: string;
    credential: string | null;
    isMedia?: boolean;
  }
  
  export interface WebRTCProxyConnection {
    type: MediaType.Type.WebRTCProxyConnection;
    url: string;
    audioUrl?: string | null;
    isMedia?: boolean;
  }
}

export interface RunInfo {
  id: number;
  result: RunResult | null;
  percentage: number;
  problemId: number;
  teamId: number;
  time: number;
  featuredRunMedia?: MediaType | null;
  reactionVideos?: MediaType[];
  isHidden?: boolean;
}

export type RunResult =
  | RunResult.ICPC
  | RunResult.IOI;

export namespace RunResult {
  export enum Type {
    ICPC = "ICPC",
    IOI = "IOI",
  }
  
  export interface ICPC {
    type: RunResult.Type.ICPC;
    verdict: Verdict;
    isFirstToSolveRun: boolean;
  }
  
  export interface IOI {
    type: RunResult.Type.IOI;
    score: number[];
    wrongVerdict?: Verdict | null;
    difference?: number;
    scoreAfter?: number;
    isFirstBestRun?: boolean;
    isFirstBestTeamRun?: boolean;
  }
}

export interface Verdict {
  shortName: string;
  isAddingPenalty: boolean;
  isAccepted: boolean;
}

export interface Scoreboard {
  type: ScoreboardUpdateType;
  rows: PersistentMap;
  order: number[];
  ranks: number[];
  awards: Map<Award, number[]>;
}

export enum ScoreboardUpdateType {
  DIFF = "DIFF",
  SNAPSHOT = "SNAPSHOT",
}

export type PersistentMap = any;

export type Award =
  | Award.group_champion
  | Award.medal;

export namespace Award {
  export enum Type {
    group_champion = "group_champion",
    medal = "medal",
  }
  
  export interface group_champion {
    type: Award.Type.group_champion;
    group: string;
  }
  
  export interface medal {
    type: Award.Type.medal;
    medalType: string;
  }
}

export interface LegacyScoreboard {
  rows: LegacyScoreboardRow[];
}

export interface LegacyScoreboardRow {
  teamId: number;
  rank: number;
  totalScore: number;
  penalty: number;
  lastAccepted: number;
  medalType: string | null;
  problemResults: ProblemResult[];
  teamGroups: string[];
  championInGroups: string[];
}

export type ProblemResult =
  | ProblemResult.ICPC
  | ProblemResult.IOI;

export namespace ProblemResult {
  export enum Type {
    ICPC = "ICPC",
    IOI = "IOI",
  }
  
  export interface ICPC {
    type: ProblemResult.Type.ICPC;
    wrongAttempts: number;
    pendingAttempts: number;
    isSolved: boolean;
    isFirstToSolve: boolean;
    lastSubmitTimeMs: number | null;
  }
  
  export interface IOI {
    type: ProblemResult.Type.IOI;
    score: number | null;
    lastSubmitTimeMs: number | null;
    isFirstBest: boolean;
  }
}

export type MainScreenEvent =
  | MainScreenEvent.HideWidget
  | MainScreenEvent.MainScreenSnapshot
  | MainScreenEvent.ShowWidget;

export namespace MainScreenEvent {
  export enum Type {
    HideWidget = "HideWidget",
    MainScreenSnapshot = "MainScreenSnapshot",
    ShowWidget = "ShowWidget",
  }
  
  export interface HideWidget {
    type: MainScreenEvent.Type.HideWidget;
    id: string;
  }
  
  export interface MainScreenSnapshot {
    type: MainScreenEvent.Type.MainScreenSnapshot;
    widgets: Widget[];
  }
  
  export interface ShowWidget {
    type: MainScreenEvent.Type.ShowWidget;
    widget: Widget;
  }
}

export type Widget =
  | Widget.AdvertisementWidget
  | Widget.FullScreenClockWidget
  | Widget.PictureWidget
  | Widget.QueueWidget
  | Widget.ScoreboardWidget
  | Widget.StatisticsWidget
  | Widget.SvgWidget
  | Widget.TeamLocatorWidget
  | Widget.TeamViewWidget
  | Widget.TickerWidget;

export namespace Widget {
  export enum Type {
    AdvertisementWidget = "AdvertisementWidget",
    FullScreenClockWidget = "FullScreenClockWidget",
    PictureWidget = "PictureWidget",
    QueueWidget = "QueueWidget",
    ScoreboardWidget = "ScoreboardWidget",
    StatisticsWidget = "StatisticsWidget",
    SvgWidget = "SvgWidget",
    TeamLocatorWidget = "TeamLocatorWidget",
    TeamViewWidget = "TeamViewWidget",
    TickerWidget = "TickerWidget",
  }
  
  export interface AdvertisementWidget {
    type: Widget.Type.AdvertisementWidget;
    widgetId: string;
    location: LocationRectangle;
    advertisement: AdvertisementSettings;
  }
  
  export interface FullScreenClockWidget {
    type: Widget.Type.FullScreenClockWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: FullScreenClockSettings;
  }
  
  export interface PictureWidget {
    type: Widget.Type.PictureWidget;
    widgetId: string;
    location: LocationRectangle;
    picture: PictureSettings;
  }
  
  export interface QueueWidget {
    type: Widget.Type.QueueWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: QueueSettings;
  }
  
  export interface ScoreboardWidget {
    type: Widget.Type.ScoreboardWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: ScoreboardSettings;
  }
  
  export interface StatisticsWidget {
    type: Widget.Type.StatisticsWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: StatisticsSettings;
  }
  
  export interface SvgWidget {
    type: Widget.Type.SvgWidget;
    widgetId: string;
    location: LocationRectangle;
    content: string;
  }
  
  export interface TeamLocatorWidget {
    type: Widget.Type.TeamLocatorWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: TeamLocatorSettings;
  }
  
  export interface TeamViewWidget {
    type: Widget.Type.TeamViewWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: OverlayTeamViewSettings;
  }
  
  export interface TickerWidget {
    type: Widget.Type.TickerWidget;
    widgetId: string;
    location: LocationRectangle;
    settings: TickerSettings;
  }
}

export interface LocationRectangle {
  positionX: number;
  positionY: number;
  sizeX: number;
  sizeY: number;
}

export interface AdvertisementSettings {
  text: string;
}

export interface FullScreenClockSettings {
  globalTimeMode?: boolean;
  quietMode?: boolean;
  contestCountdownMode?: boolean;
}

export interface PictureSettings {
  url: string;
  name: string;
}

export interface QueueSettings {
}

export interface ScoreboardSettings {
  isInfinite?: boolean;
  numRows?: number | null;
  startFromRow?: number;
  optimismLevel?: OptimismLevel;
  teamsOnPage?: number;
  group?: string;
}

export interface StatisticsSettings {
}

export interface TeamLocatorSettings {
  circles?: TeamLocatorCircleSettings[];
  scene?: string;
}

export interface OverlayTeamViewSettings {
  content?: MediaType[];
  position?: TeamViewPosition;
}

export interface TickerSettings {
}

export enum OptimismLevel {
  normal = "normal",
  optimistic = "optimistic",
  pessimistic = "pessimistic",
}

export enum TeamViewPosition {
  SINGLE_TOP_RIGHT = "SINGLE_TOP_RIGHT",
  PVP_TOP = "PVP_TOP",
  PVP_BOTTOM = "PVP_BOTTOM",
  TOP_LEFT = "TOP_LEFT",
  TOP_RIGHT = "TOP_RIGHT",
  BOTTOM_LEFT = "BOTTOM_LEFT",
  BOTTOM_RIGHT = "BOTTOM_RIGHT",
}

export interface TeamLocatorCircleSettings {
  x: number;
  y: number;
  radius: number;
  teamId: number;
}

export type QueueEvent =
  | QueueEvent.AddRunToQueue
  | QueueEvent.ModifyRunInQueue
  | QueueEvent.QueueSnapshot
  | QueueEvent.RemoveRunFromQueue;

export namespace QueueEvent {
  export enum Type {
    AddRunToQueue = "AddRunToQueue",
    ModifyRunInQueue = "ModifyRunInQueue",
    QueueSnapshot = "QueueSnapshot",
    RemoveRunFromQueue = "RemoveRunFromQueue",
  }
  
  export interface AddRunToQueue {
    type: QueueEvent.Type.AddRunToQueue;
    info: RunInfo;
  }
  
  export interface ModifyRunInQueue {
    type: QueueEvent.Type.ModifyRunInQueue;
    info: RunInfo;
  }
  
  export interface QueueSnapshot {
    type: QueueEvent.Type.QueueSnapshot;
    infos: RunInfo[];
  }
  
  export interface RemoveRunFromQueue {
    type: QueueEvent.Type.RemoveRunFromQueue;
    info: RunInfo;
  }
}

export type AnalyticsEvent =
  | AnalyticsEvent.AddAnalyticsMessage
  | AnalyticsEvent.AnalyticsMessageSnapshot
  | AnalyticsEvent.ModifyAnalyticsMessage;

export namespace AnalyticsEvent {
  export enum Type {
    AddAnalyticsMessage = "AddAnalyticsMessage",
    AnalyticsMessageSnapshot = "AnalyticsMessageSnapshot",
    ModifyAnalyticsMessage = "ModifyAnalyticsMessage",
  }
  
  export interface AddAnalyticsMessage {
    type: AnalyticsEvent.Type.AddAnalyticsMessage;
    message: AnalyticsMessage;
  }
  
  export interface AnalyticsMessageSnapshot {
    type: AnalyticsEvent.Type.AnalyticsMessageSnapshot;
    messages: AnalyticsMessage[];
  }
  
  export interface ModifyAnalyticsMessage {
    type: AnalyticsEvent.Type.ModifyAnalyticsMessage;
    message: AnalyticsMessage;
  }
}

export type AnalyticsMessage =
  | AnalyticsMessage.commentary;

export namespace AnalyticsMessage {
  export enum Type {
    commentary = "commentary",
  }
  
  export interface commentary {
    type: AnalyticsMessage.Type.commentary;
    id: string;
    message: string;
    timeUnixMs: number;
    relativeTimeMs: number;
    teamIds: number[];
    runIds: number[];
    priority?: number;
    tags?: string[];
    advertisement?: AnalyticsCompanionPreset | null;
    tickerMessage?: AnalyticsCompanionPreset | null;
    featuredRun?: AnalyticsCompanionRun | null;
  }
}

export interface AnalyticsCompanionPreset {
  presetId: number;
  expirationTimeUnixMs: number | null;
}

export interface AnalyticsCompanionRun {
  expirationTimeUnixMs: number | null;
  mediaType: MediaType;
}

export type TickerEvent =
  | TickerEvent.AddMessage
  | TickerEvent.RemoveMessage
  | TickerEvent.TickerSnapshot;

export namespace TickerEvent {
  export enum Type {
    AddMessage = "AddMessage",
    RemoveMessage = "RemoveMessage",
    TickerSnapshot = "TickerSnapshot",
  }
  
  export interface AddMessage {
    type: TickerEvent.Type.AddMessage;
    message: TickerMessage;
  }
  
  export interface RemoveMessage {
    type: TickerEvent.Type.RemoveMessage;
    messageId: string;
  }
  
  export interface TickerSnapshot {
    type: TickerEvent.Type.TickerSnapshot;
    messages: TickerMessage[];
  }
}

export type TickerMessage =
  | TickerMessage.clock
  | TickerMessage.scoreboard
  | TickerMessage.text;

export namespace TickerMessage {
  export enum Type {
    clock = "clock",
    scoreboard = "scoreboard",
    text = "text",
  }
  
  export interface clock {
    type: TickerMessage.Type.clock;
    id: string;
    part: TickerPart;
    periodMs: number;
    settings: clock;
  }
  
  export interface scoreboard {
    type: TickerMessage.Type.scoreboard;
    id: string;
    part: TickerPart;
    periodMs: number;
    settings: scoreboard;
  }
  
  export interface text {
    type: TickerMessage.Type.text;
    id: string;
    part: TickerPart;
    periodMs: number;
    settings: text;
  }
}

export enum TickerPart {
  short = "short",
  long = "long",
}

export interface clock {
  part: TickerPart;
  periodMs: number;
}

export interface scoreboard {
  part: TickerPart;
  periodMs: number;
  from: number;
  to: number;
}

export interface text {
  part: TickerPart;
  periodMs: number;
  text: string;
}
