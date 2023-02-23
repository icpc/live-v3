export const DEBUG = process.env.NODE_ENV === "development";

export const WEBSOCKETS = {
    mainScreen: "mainScreen",
    contestInfo: "contestInfo",
    queue: "queue",
    statistics: "statistics",
    ticker: "ticker",
    scoreboardNormal: "scoreboard/normal",
    scoreboardOptimistic: "scoreboard/optimistic",
    scoreboardPessimistic: "scoreboard/pessimistic",
};

export const SCOREBOARD_TYPES = Object.freeze({
    normal: "normal",
    optimistic: "optimistic",
    pessimistic: "pessimistic"
});
