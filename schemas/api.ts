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
  lastSubmitTime: number;
  rows: ScoreboardRow[];
}

export interface ScoreboardRow {
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
