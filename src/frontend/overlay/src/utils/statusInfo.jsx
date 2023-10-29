import c from "../config";

export const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});

export const TeamTaskSymbol = Object.freeze({
    [TeamTaskStatus.solved]: "+",
    [TeamTaskStatus.failed]: "-",
    [TeamTaskStatus.untouched]: "",
    [TeamTaskStatus.unknown]: "?",
    [TeamTaskStatus.first]: "+",
});

export function getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
    if (isFirstToSolve) {
        return TeamTaskStatus.first;
    } else if (isSolved) {
        return TeamTaskStatus.solved;
    } else if (pendingAttempts > 0) {
        return TeamTaskStatus.unknown;
    } else if (wrongAttempts > 0) {
        return TeamTaskStatus.failed;
    } else {
        return TeamTaskStatus.untouched;
    }
}

export const TeamTaskColor = Object.freeze({
    [TeamTaskStatus.solved]: c.VERDICT_OK,
    [TeamTaskStatus.failed]: c.VERDICT_NOK,
    // [TeamTaskStatus.untouched]: c.STATISTICS_BG_COLOR,
    [TeamTaskStatus.unknown]: c.VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: c.VERDICT_OK,
});

export const getTeamTaskColor = (score, minScore, maxScore) => {
    if (score === undefined) {
        return c.CELL_BG_COLOR;
    }
    if (minScore !== undefined && maxScore !== undefined) {
        const [minRed, minGreen, minBlue] = [203, 46, 40];
        const [maxRed, maxGreen, maxBlue] = [46, 203, 104];

        const scoreDiff = maxScore - minScore;
        const redDiff = maxRed - minRed;
        const greenDiff = maxGreen - minGreen;
        const blueDiff = maxBlue - minBlue;

        const middleRange = mapNumber(score, minScore, maxScore, 0, Math.PI);
        const middleFactor = 90;

        const [red, green, blue] = [
            Math.min(minRed + score * (redDiff / scoreDiff) + (middleFactor * Math.sin(middleRange)), 255),
            Math.min(minGreen + score * (greenDiff / scoreDiff) + (middleFactor * Math.sin(middleRange)), 255),
            Math.min(minBlue + score * (blueDiff / scoreDiff) + ((middleFactor * Math.sin(middleRange)) / 10), 255)
        ];

        return `#${((1 << 24) + (red << 16) + (green << 8) + blue).toString(16).slice(1, 7)}`;
    }

    return undefined;
};

const mapNumber = (value, oldMin, oldMax, newMin, newMax) => {
    const result = (value - oldMin) * (newMax - newMin) / (oldMax - oldMin) + newMin;
    return Math.min(Math.max(result, newMin), newMax);
};

