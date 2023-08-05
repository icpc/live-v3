export type Verdict =
    { shortName: "AC", isAddingPenalty: false, isAccepted: true } |
    { shortName: "RJ", isAddingPenalty: true, isAccepted: false } |
    { shortName: "FL", isAddingPenalty: false, isAccepted: true } |
    { shortName: "CE", isAddingPenalty: false, isAccepted: false } |
    { shortName: "CE", isAddingPenalty: true, isAccepted: false } |
    { shortName: "PE", isAddingPenalty: true, isAccepted: false } |
    { shortName: "RE", isAddingPenalty: true, isAccepted: false } |
    { shortName: "TL", isAddingPenalty: true, isAccepted: false } |
    { shortName: "ML", isAddingPenalty: true, isAccepted: false } |
    { shortName: "OL", isAddingPenalty: true, isAccepted: false } |
    { shortName: "IL", isAddingPenalty: true, isAccepted: false } |
    { shortName: "SV", isAddingPenalty: true, isAccepted: false } |
    { shortName: "IG", isAddingPenalty: false, isAccepted: false } |
    { shortName: "CH", isAddingPenalty: true, isAccepted: false } |
    { shortName: "WA", isAddingPenalty: true, isAccepted: false }

export interface IOIRunResult {
    score: number[],
    wrongVerdict?: Verdict,
    difference: number,
    scoreAfter: number,
    isFirstBestRun: boolean,
    isFirstBestTeamRun: boolean
}


export interface ICPCRunResult {
    verdict: Verdict,
    isFirstToSolveRun: boolean
}

export type RunResult = {
    "ICPC": ICPCRunResult
} | {
    "IOI": IOIRunResult
};

export type MediaType = any; // sorry

export interface RunInfo {
    id: number,
    result?: RunResult,
    percentage: number
    problemId: number,
    teamId: number,
    time: number // in milliseconds
    featuredRunMedia?: MediaType,
    reactionVideos: MediaType[],
    isHidden: boolean
}
