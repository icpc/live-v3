export interface ContestInfo {
  name: string;
  status: ContestStatus;
  resultType: ContestResultType;
  contestLengthMs: number;
  freezeTimeMs: number | null;
  problems: ProblemInfo[];
  teams: TeamInfo[];
  groups: GroupInfo[];
  organizations: OrganizationInfo[];
  languages: LanguageInfo[];
  penaltyRoundingMode: PenaltyRoundingMode;
  emulationSpeed: number;
  awardsSettings: AwardsSettings;
  penaltyPerWrongAttemptSeconds: number;
  queueSettings: QueueSettings;
  showTeamsWithoutSubmissions: boolean;
}

export type ContestStatus =
  | ContestStatus.before
  | ContestStatus.finalized
  | ContestStatus.over
  | ContestStatus.running;

export namespace ContestStatus {
  export enum Type {
    before = "before",
    finalized = "finalized",
    over = "over",
    running = "running",
  }
  
  export interface before {
    type: ContestStatus.Type.before;
    holdTimeMs?: number | null;
    scheduledStartAtUnixMs?: number | null;
  }
  
  export interface finalized {
    type: ContestStatus.Type.finalized;
    startedAtUnixMs: number;
    finishAtUnixMs: number;
    finalizedAtUnixMs: number;
    frozenAtUnixMs?: number | null;
  }
  
  export interface over {
    type: ContestStatus.Type.over;
    startedAtUnixMs: number;
    finishAtUnixMs: number;
    frozenAtUnixMs?: number | null;
  }
  
  export interface running {
    type: ContestStatus.Type.running;
    startedAtUnixMs: number;
    frozenAtUnixMs?: number | null;
  }
}

export enum ContestResultType {
  ICPC = "ICPC",
  IOI = "IOI",
}

export enum PenaltyRoundingMode {
  each_submission_down_to_minute = "each_submission_down_to_minute",
  each_submission_up_to_minute = "each_submission_up_to_minute",
  sum_down_to_minute = "sum_down_to_minute",
  sum_in_seconds = "sum_in_seconds",
  last = "last",
  zero = "zero",
}

export interface AwardsSettings {
  firstToSolveProblems?: boolean;
  championTitle?: string | null;
  groupsChampionTitles?: { [key: GroupId]: string };
  rankAwardsMaxRank?: number;
  medalGroups?: MedalGroup[];
  manual?: ManualAwardSetting[];
}

export interface QueueSettings {
  waitTimeSeconds?: number;
  firstToSolveWaitTimeSeconds?: number;
  featuredRunWaitTimeSeconds?: number;
  inProgressRunWaitTimeSeconds?: number;
  maxQueueSize?: number;
  maxUntestedRun?: number;
}

export interface ProblemInfo {
  id: ProblemId;
  letter: string;
  name: string;
  ordinal: number;
  minScore: number | null;
  maxScore: number | null;
  color: string | null;
  unsolvedColor: string | null;
  scoreMergeMode: ScoreMergeMode | null;
  isHidden: boolean;
  weight: number;
  ftsMode: FtsMode;
}

export interface TeamInfo {
  id: TeamId;
  name: string;
  shortName: string;
  groups: GroupId[];
  hashTag: string | null;
  medias: { [key in TeamMediaType]: MediaType };
  isHidden: boolean;
  isOutOfContest: boolean;
  organizationId: OrganizationId | null;
  color?: string | null;
  customFields: { [key: string]: string };
}

export interface GroupInfo {
  id: GroupId;
  displayName: string;
  isHidden: boolean;
  isOutOfContest: boolean;
}

export interface OrganizationInfo {
  id: OrganizationId;
  displayName: string;
  fullName: string;
  logo: MediaType | null;
}

export interface LanguageInfo {
  id: LanguageId;
  name: string;
  extensions: string[];
}

export type ProblemId = string;

export enum ScoreMergeMode {
  MAX_PER_GROUP = "MAX_PER_GROUP",
  MAX_TOTAL = "MAX_TOTAL",
  LAST = "LAST",
  LAST_OK = "LAST_OK",
  SUM = "SUM",
}

export type FtsMode =
  | FtsMode.auto
  | FtsMode.custom
  | FtsMode.hidden;

export namespace FtsMode {
  export enum Type {
    auto = "auto",
    custom = "custom",
    hidden = "hidden",
  }
  
  export interface auto {
    type: FtsMode.Type.auto;
  }
  
  export interface custom {
    type: FtsMode.Type.custom;
    runId: RunId;
  }
  
  export interface hidden {
    type: FtsMode.Type.hidden;
  }
}

export type TeamId = string;

export type OrganizationId = string;

export type GroupId = string;

export type MediaType =
  | MediaType.Audio
  | MediaType.HLSVideo
  | MediaType.Image
  | MediaType.M2tsVideo
  | MediaType.Object
  | MediaType.Video
  | MediaType.WebRTCGrabberConnection
  | MediaType.WebRTCProxyConnection;

export namespace MediaType {
  export enum Type {
    Audio = "Audio",
    HLSVideo = "HLSVideo",
    Image = "Image",
    M2tsVideo = "M2tsVideo",
    Object = "Object",
    Video = "Video",
    WebRTCGrabberConnection = "WebRTCGrabberConnection",
    WebRTCProxyConnection = "WebRTCProxyConnection",
  }
  
  export interface Audio {
    type: MediaType.Type.Audio;
    url: string;
  }
  
  export interface HLSVideo {
    type: MediaType.Type.HLSVideo;
    url: string;
    jwtToken?: string | null;
    vertical?: boolean;
  }
  
  export interface Image {
    type: MediaType.Type.Image;
    url: string;
    vertical?: boolean;
  }
  
  export interface M2tsVideo {
    type: MediaType.Type.M2tsVideo;
    url: string;
    vertical?: boolean;
  }
  
  export interface Object {
    type: MediaType.Type.Object;
    url: string;
  }
  
  export interface Video {
    type: MediaType.Type.Video;
    url: string;
    vertical?: boolean;
  }
  
  export interface WebRTCGrabberConnection {
    type: MediaType.Type.WebRTCGrabberConnection;
    url: string;
    peerName: string;
    streamType: string;
    credential: string | null;
    vertical?: boolean;
  }
  
  export interface WebRTCProxyConnection {
    type: MediaType.Type.WebRTCProxyConnection;
    url: string;
    audioUrl?: string | null;
    vertical?: boolean;
  }
}

export type LanguageId = string;

export interface MedalGroup {
  medals: MedalSettings[];
  groups?: GroupId[];
  excludedGroups?: GroupId[];
}

export interface ManualAwardSetting {
  id: string;
  citation: string;
  teamCdsIds: TeamId[];
}

export type RunId = string;

export enum TeamMediaType {
  camera = "camera",
  screen = "screen",
  record = "record",
  photo = "photo",
  reactionVideo = "reactionVideo",
  achievement = "achievement",
  audio = "audio",
}

export interface MedalSettings {
  id: string;
  citation: string;
  color?: MedalColor | null;
  maxRank?: number | null;
  minScore?: number | null;
  tiebreakMode?: MedalTiebreakMode;
}

export enum MedalColor {
  GOLD = "GOLD",
  SILVER = "SILVER",
  BRONZE = "BRONZE",
}

export enum MedalTiebreakMode {
  NONE = "NONE",
  ALL = "ALL",
}

export interface RunInfo {
  id: RunId;
  result: RunResult;
  problemId: ProblemId;
  teamId: TeamId;
  time: number;
  languageId: LanguageId | null;
  testedTime?: number | null;
  featuredRunMedia: MediaType | null;
  reactionVideos: MediaType[];
  isHidden: boolean;
}

export type RunResult =
  | RunResult.ICPC
  | RunResult.IN_PROGRESS
  | RunResult.IOI;

export namespace RunResult {
  export enum Type {
    ICPC = "ICPC",
    IOI = "IOI",
    IN_PROGRESS = "IN_PROGRESS",
  }
  
  export interface ICPC {
    type: RunResult.Type.ICPC;
    verdict: Verdict;
    isFirstToSolveRun: boolean;
  }
  
  export interface IOI {
    type: RunResult.Type.IOI;
    score: number[];
    wrongVerdict: Verdict | null;
    difference: number;
    scoreAfter: number;
    isFirstBestRun: boolean;
    isFirstBestTeamRun: boolean;
  }
  
  export interface IN_PROGRESS {
    type: RunResult.Type.IN_PROGRESS;
    testedPart: number;
  }
}

export interface Verdict {
  shortName: string;
  isAddingPenalty: boolean;
  isAccepted: boolean;
}

export interface ScoreboardDiff {
  rows: { [key: TeamId]: ScoreboardRow };
  order: TeamId[];
  ranks: number[];
  awards: Award[];
}

export interface ScoreboardRow {
  totalScore: number;
  penalty: number;
  lastAcceptedMs: number;
  problemResults: ProblemResult[];
}

export type Award =
  | Award.custom
  | Award.group_champion
  | Award.medal
  | Award.winner;

export namespace Award {
  export enum Type {
    custom = "custom",
    group_champion = "group_champion",
    medal = "medal",
    winner = "winner",
  }
  
  export interface custom {
    type: Award.Type.custom;
    id: string;
    citation: string;
    teams: TeamId[];
  }
  
  export interface group_champion {
    type: Award.Type.group_champion;
    id: string;
    citation: string;
    groupId: GroupId;
    teams: TeamId[];
  }
  
  export interface medal {
    type: Award.Type.medal;
    id: string;
    citation: string;
    medalColor: MedalColor | null;
    teams: TeamId[];
  }
  
  export interface winner {
    type: Award.Type.winner;
    id: string;
    citation: string;
    teams: TeamId[];
  }
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
    pendingAttempts: number;
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
    widgetLocationId: string;
    statisticsId: string;
    advertisement: AdvertisementSettings;
  }
  
  export interface FullScreenClockWidget {
    type: Widget.Type.FullScreenClockWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: FullScreenClockSettings;
  }
  
  export interface PictureWidget {
    type: Widget.Type.PictureWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    picture: PictureSettings;
  }
  
  export interface QueueWidget {
    type: Widget.Type.QueueWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: QueueSettings;
  }
  
  export interface ScoreboardWidget {
    type: Widget.Type.ScoreboardWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: ScoreboardSettings;
  }
  
  export interface StatisticsWidget {
    type: Widget.Type.StatisticsWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: StatisticsSettings;
  }
  
  export interface SvgWidget {
    type: Widget.Type.SvgWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    content: string;
  }
  
  export interface TeamLocatorWidget {
    type: Widget.Type.TeamLocatorWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: TeamLocatorSettings;
  }
  
  export interface TeamViewWidget {
    type: Widget.Type.TeamViewWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: OverlayTeamViewSettings;
  }
  
  export interface TickerWidget {
    type: Widget.Type.TickerWidget;
    widgetId: string;
    widgetLocationId: string;
    statisticsId: string;
    settings: TickerSettings;
  }
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
  horizontal?: boolean;
}

export interface ScoreboardSettings {
  scrollDirection?: ScoreboardScrollDirection;
  optimismLevel?: OptimismLevel;
  group?: string;
}

export interface StatisticsSettings {
}

export interface TeamLocatorSettings {
  circles?: TeamLocatorCircleSettings[];
  scene?: string;
}

export interface OverlayTeamViewSettings {
  teamId: TeamId;
  primary: MediaType | null;
  secondary: MediaType | null;
  showTaskStatus: boolean;
  achievement: MediaType | null;
  showTimeLine: boolean;
  position: TeamViewPosition;
}

export interface TickerSettings {
}

export enum ScoreboardScrollDirection {
  FirstPage = "FirstPage",
  Back = "Back",
  Pause = "Pause",
  Forward = "Forward",
  LastPage = "LastPage",
}

export enum OptimismLevel {
  normal = "normal",
  optimistic = "optimistic",
  pessimistic = "pessimistic",
}

export enum TeamViewPosition {
  SINGLE = "SINGLE",
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
  teamId: TeamId;
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
  | AnalyticsEvent.AnalyticsMessageSnapshot
  | AnalyticsEvent.UpdateAnalyticsMessage;

export namespace AnalyticsEvent {
  export enum Type {
    AnalyticsMessageSnapshot = "AnalyticsMessageSnapshot",
    UpdateAnalyticsMessage = "UpdateAnalyticsMessage",
  }
  
  export interface AnalyticsMessageSnapshot {
    type: AnalyticsEvent.Type.AnalyticsMessageSnapshot;
    messages: AnalyticsMessage[];
  }
  
  export interface UpdateAnalyticsMessage {
    type: AnalyticsEvent.Type.UpdateAnalyticsMessage;
    message: AnalyticsMessage;
  }
}

export interface AnalyticsMessage {
  id: AnalyticsMessageId;
  updateTimeUnixMs: number;
  timeUnixMs: number;
  relativeTimeMs: number;
  comments: AnalyticsMessageComment[];
  teamId: TeamId | null;
  runInfo: RunInfo | null;
  featuredRun: AnalyticsCompanionRun | null;
  tags: string[];
}

export type AnalyticsMessageId = string;

export interface AnalyticsCompanionRun {
  expirationTimeUnixMs: number | null;
  mediaType: MediaType;
}

export interface AnalyticsMessageComment {
  id: CommentaryMessageId;
  message: string;
  advertisement: AnalyticsCompanionPreset | null;
  tickerMessage: AnalyticsCompanionPreset | null;
  creationTimeUnixMs: number;
}

export type CommentaryMessageId = string;

export interface AnalyticsCompanionPreset {
  presetId: number;
  expirationTimeUnixMs: number | null;
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
  | TickerMessage.empty
  | TickerMessage.image
  | TickerMessage.scoreboard
  | TickerMessage.text;

export namespace TickerMessage {
  export enum Type {
    clock = "clock",
    empty = "empty",
    image = "image",
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
  
  export interface empty {
    type: TickerMessage.Type.empty;
    id: string;
    part: TickerPart;
    periodMs: number;
    settings: empty;
  }
  
  export interface image {
    type: TickerMessage.Type.image;
    id: string;
    part: TickerPart;
    periodMs: number;
    settings: image;
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
  timeZone?: string | null;
}

export interface empty {
  part: TickerPart;
  periodMs: number;
}

export interface image {
  part: TickerPart;
  periodMs: number;
  path: string;
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

export type SolutionsStatistic =
  | SolutionsStatistic.ICPC
  | SolutionsStatistic.IOI;

export namespace SolutionsStatistic {
  export enum Type {
    ICPC = "ICPC",
    IOI = "IOI",
  }
  
  export interface ICPC {
    type: SolutionsStatistic.Type.ICPC;
    teamsCount: number;
    stats: ICPCProblemSolutionsStatistic[];
  }
  
  export interface IOI {
    type: SolutionsStatistic.Type.IOI;
    teamsCount: number;
    stats: IOIProblemSolutionsStatistic[];
  }
}

export interface ICPCProblemSolutionsStatistic {
  success: number;
  wrong: number;
  pending: number;
}

export interface IOIProblemSolutionsStatistic {
  result: IOIProblemEntity[];
  pending: number;
}

export interface IOIProblemEntity {
  count: number;
  score: number;
}

export interface ExternalTeamViewSettings {
  teamId?: TeamId | null;
  mediaTypes?: TeamMediaType[];
  showTaskStatus?: boolean;
  showAchievement?: boolean;
  showTimeLine?: boolean;
  position?: TeamViewPosition;
}

export type ObjectSettings = any;

export interface WidgetUsageStatistics {
  entries: { [key: string]: WidgetUsageStatisticsEntry };
}

export type WidgetUsageStatisticsEntry =
  | WidgetUsageStatisticsEntry.per_team
  | WidgetUsageStatisticsEntry.simple;

export namespace WidgetUsageStatisticsEntry {
  export enum Type {
    per_team = "per_team",
    simple = "simple",
  }
  
  export interface per_team {
    type: WidgetUsageStatisticsEntry.Type.per_team;
    byTeam: { [key: TeamId]: WidgetUsageStatisticsEntry };
  }
  
  export interface simple {
    type: WidgetUsageStatisticsEntry.Type.simple;
    totalShownTimeSeconds: number;
  }
}
