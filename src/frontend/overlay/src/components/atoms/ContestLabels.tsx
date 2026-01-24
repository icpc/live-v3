import React from "react";
import styled from "styled-components";
import c from "../../config";
import { isShouldUseDarkColor } from "../../utils/colors";
import { ShrinkingBox } from "./ShrinkingBox";

import {
    TeamTaskColor,
    TeamTaskSymbol,
    TeamTaskStatus,
    getStatus,
    getIOIColor,
} from "../../utils/statusInfo";
import { formatScore } from "../../services/displayUtils";
import { conditionalShimmerStyles } from "../../utils/shimmerStyles";
import { RunInfo, ProblemResult, QueueRunInfo } from "@shared/api";
import { AwardEffect } from "@/utils/awards";

const VerdictLabel = styled(ShrinkingBox)<{ color: string }>`
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: ${c.VERDICT_LABEL_FONT_SIZE};
    font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
    background-color: ${({ color }) => color};
`;

interface ICPCVerdictLabelProps {
    runResult: Extract<RunInfo["result"], { type: "ICPC" }>;
    className?: string;
}

const ICPCVerdictLabel = ({ runResult, className }: ICPCVerdictLabelProps) => {
    const color = runResult?.verdict.isAccepted ? c.VERDICT_OK : c.VERDICT_NOK;
    return (
        <VerdictLabel
            text={runResult?.verdict.shortName ?? "??"}
            color={color}
            align="center"
            className={className}
        />
    );
};

const getIOIScoreText = (difference: number): [string, string] => {
    if (difference > 0) {
        return [`+${formatScore(difference, 1)}`, c.VERDICT_OK];
    }
    if (difference < 0) {
        return [`-${formatScore(-difference, 1)}`, c.VERDICT_NOK];
    }
    return ["=", c.VERDICT_UNKNOWN];
};

interface IOIVerdictLabelProps {
    runResult: Extract<RunInfo["result"], { type: "IOI" }>;
    className?: string;
}

const IOIVerdictLabel = ({
    runResult: { wrongVerdict, difference },
    className,
}: IOIVerdictLabelProps) => {
    const [diffText, diffColor] = getIOIScoreText(difference);
    return (
        <>
            {wrongVerdict !== undefined && (
                <VerdictLabel
                    text={wrongVerdict.shortName ?? "??"}
                    color={c.VERDICT_NOK}
                    align="center"
                    className={className}
                />
            )}
            {wrongVerdict === undefined && (
                <VerdictLabel
                    text={diffText ?? "??"}
                    color={diffColor}
                    align="center"
                    className={className}
                />
            )}
        </>
    );
};

const formatRank = (rank: number | undefined | null): string => {
    if (rank === undefined || rank == null) return "??";
    else if (rank === 0) return "*";
    return rank.toString();
};

const RankLabelWrap = styled(ShrinkingBox)<{ color: string; dark: boolean }>`
    display: flex;
    align-items: center;
    justify-content: center;

    color: ${(props) => (props.dark ? "#000" : "#FFF")};

    background-color: ${({ color }) => color};
`;

interface RankLabelProps {
    rank: number | undefined | null;
    effects?: AwardEffect[];
    className?: string;
    bg_color?: string;
}

export const RankLabel = ({
    rank,
    effects,
    className,
    bg_color,
}: RankLabelProps) => {
    const color =
        effects
            ?.map((effect) => c.EFFECT_RANK_BACKGROUND_COLOR[effect])
            .find((color) => color !== undefined) ?? bg_color;
    const dark = isShouldUseDarkColor(color ?? "");
    return (
        <RankLabelWrap
            color={color ?? ""}
            className={className}
            dark={dark}
            text={formatRank(rank)}
        />
    );
};

const VerdictCellProgressBar2 = styled.div.attrs<{ width: string }>(
    ({ width }) => ({
        style: {
            width,
        },
    }),
)<{ width: string }>`
    height: 100%;
    background-color: ${c.VERDICT_UNKNOWN};
    transition: width ${c.VERDICT_CELL_TRANSITION_TIME} linear;
`;

const VerdictCellInProgressWrap2 = styled.div`
    flex-direction: row;
    align-content: center;
    justify-content: flex-start;

    box-sizing: border-box;
    height: 100%;

    border: 3px solid ${c.VERDICT_UNKNOWN};
    border-radius: 0 ${c.VERDICT_CELL_BRODER_RADIUS}
        ${c.VERDICT_CELL_BRODER_RADIUS} 0;
`;

interface VerdictCellInProgress2Props {
    percentage: number;
    className?: string;
}

const VerdictCellInProgress2 = ({
    percentage,
    className,
}: VerdictCellInProgress2Props) => {
    return (
        <VerdictCellInProgressWrap2 className={className}>
            {percentage !== 0 && (
                <VerdictCellProgressBar2 width={percentage * 100 + "%"} />
            )}
        </VerdictCellInProgressWrap2>
    );
};

interface RunStatusLabelProps {
    runInfo: RunInfo | QueueRunInfo;
    className?: string;
}

export const RunStatusLabel = ({ runInfo, className }: RunStatusLabelProps) => {
    return (
        <>
            {runInfo.result.type === "ICPC" && (
                <ICPCVerdictLabel
                    runResult={runInfo.result}
                    className={className}
                />
            )}
            {runInfo.result.type === "IOI" && (
                <IOIVerdictLabel
                    runResult={runInfo.result}
                    className={className}
                />
            )}
            {runInfo.result.type === "IN_PROGRESS" && (
                <VerdictCellInProgress2
                    percentage={runInfo.result.testedPart}
                    className={className}
                />
            )}
        </>
    );
};

const TaskResultLabelWrapper2 = styled.div<{
    color: string;
    isShimmering: boolean;
    problemColor?: string;
    isFake?: boolean;
}>`
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    font-weight: bold;
    color: #fff;

    ${({ color, isShimmering, problemColor, isFake }) =>
        conditionalShimmerStyles(isShimmering && !isFake, problemColor, color)}
`;
const AttemptsOrScoreLabelWrapper = styled.div`
    position: absolute;
`;

interface ICPCTaskResultLabel2Props {
    problemColor?: string;
    problemResult: Extract<ProblemResult, { type: "ICPC" }>;
    problemLetter?: string;
    className?: string;
}

const ICPCTaskResultLabel2 = ({
    problemColor,
    problemResult: r,
    className,
}: ICPCTaskResultLabel2Props) => {
    const status = getStatus(
        r.isFirstToSolve,
        r.isSolved,
        r.pendingAttempts,
        r.wrongAttempts,
    );
    const attempts = r.wrongAttempts + r.pendingAttempts;
    const isShimmering = status === TeamTaskStatus.first;

    return (
        <>
            <TaskResultLabelWrapper2
                color={TeamTaskColor[status]}
                isShimmering={isShimmering}
                problemColor={problemColor}
                className={className}
            >
                <AttemptsOrScoreLabelWrapper>
                    {TeamTaskSymbol[status]}
                    {status !== TeamTaskStatus.untouched &&
                        attempts > 0 &&
                        attempts}
                </AttemptsOrScoreLabelWrapper>
            </TaskResultLabelWrapper2>
        </>
    );
};

interface IOITaskResultLabel2Props {
    problemColor?: string;
    problemResult: Extract<ProblemResult, { type: "IOI" }>;
    problemLetter?: string;
    minScore?: number;
    maxScore?: number;
    className?: string;
}

const IOITaskResultLabel2 = ({
    problemColor,
    problemResult: r,
    minScore,
    maxScore,
    className,
}: IOITaskResultLabel2Props) => {
    const isShimmering = r.isFirstBest;

    return (
        <TaskResultLabelWrapper2
            color={getIOIColor(r.score, minScore, maxScore)}
            isShimmering={isShimmering}
            problemColor={problemColor}
            className={className}
        >
            <AttemptsOrScoreLabelWrapper>
                {formatScore(r?.score)}
            </AttemptsOrScoreLabelWrapper>
        </TaskResultLabelWrapper2>
    );
};

interface TaskResultLabelProps {
    problemResult: ProblemResult;
    problemLetter?: string;
    problemColor?: string;
    minScore?: number;
    maxScore?: number;
    className?: string;
    isFake?: boolean;
    isTop?: boolean;
}

export const TaskResultLabel: React.FC<TaskResultLabelProps> = ({
    problemResult,
    problemLetter,
    ...props
}) => {
    if (problemResult.type === "ICPC") {
        return (
            <ICPCTaskResultLabel2
                problemResult={problemResult}
                problemLetter={problemLetter}
                {...props}
            />
        );
    }
    if (problemResult.type === "IOI") {
        return (
            <IOITaskResultLabel2
                problemResult={problemResult}
                problemLetter={problemLetter}
                {...props}
            />
        );
    }
    return null;
};
