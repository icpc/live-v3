import React from "react";
import { useTransition } from "react-transition-state";
import styled, { css, Keyframes } from "styled-components";
import c from "@/config";
import { ShrinkingBox } from "../../../atoms/ShrinkingBox";
import { RankLabel, RunStatusLabel } from "../../../atoms/ContestLabels";
import { ProblemLabel } from "@/components/atoms/ProblemLabel";
import { formatScore } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";
import { Award, OptimismLevel } from "@shared/api";
import { isShouldUseDarkColor } from "@/utils/colors";
import { createShimmerStyles } from "@/utils/shimmerStyles";
import { QueueRowInfo } from "./utils/queueState";
import { queueRowContractionStates } from "./utils/transitionStates";

interface QueueRowAnimatorProps {
    bottom: number;
    right: number;
    horizontal?: boolean;
    zIndex: number;
    animation: Keyframes;
    fts: boolean;
}

export const QueueRowAnimator = styled.div.attrs<QueueRowAnimatorProps>(
    ({ bottom, right, zIndex }) => {
        return {
            style: {
                transform: `translate3d(${-right}px, ${-bottom}px, 0)`,
                zIndex: zIndex,
            },
        };
    },
)<QueueRowAnimatorProps>`
    overflow: hidden;
    width: ${({ horizontal }) =>
        horizontal ? c.QUEUE_ROW_WIDTH + "px" : "100%"};

    position: absolute;
    bottom: 0;
    right: 0;

    transition-duration: ${({ fts }) =>
        fts ? c.QUEUE_ROW_FTS_TRANSITION_TIME : c.QUEUE_ROW_TRANSITION_TIME}ms;
    transition-timing-function: linear;
    transition-property: transform;

    animation: ${({ animation }) => animation} ${c.QUEUE_ROW_APPEAR_TIME}ms
        linear;
    animation-fill-mode: forwards;

    will-change: transform;
`;

const QueueRankLabel = styled(RankLabel)`
    width: ${c.QUEUE_RANK_LABEL_WIDTH};
    align-self: stretch;
    padding-left: ${c.QUEUE_RANK_LABEL_PADDING_LEFT};
    flex-shrink: 0;
`;

const QueueTeamNameLabel = styled(ShrinkingBox)`
    flex-grow: 1;
`;

const QueueRunStatusLabel = styled(RunStatusLabel)`
    width: ${c.QUEUE_ROW_STATUS_LABEL_WIDTH};
    flex-shrink: 0;
`;

const StyledQueueRow = styled.div`
    width: 100%;
    height: ${c.QUEUE_ROW_HEIGHT}px;
    display: flex;
    align-items: center;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
    overflow: hidden;
    gap: ${c.QUEUE_ROW_GAP};
    color: white;
    font-size: ${c.QUEUE_ROW_FONT_SIZE};
    background: ${c.QUEUE_ROW_BACKGROUND};
`;

const QueueScoreLabel = styled(ShrinkingBox)`
    width: ${c.QUEUE_SCORE_LABEL_WIDTH};
    flex-shrink: 0;
    flex-direction: row-reverse;
`;

const QueueProblemLabel = styled(ProblemLabel)`
    width: ${c.QUEUE_ROW_PROBLEM_LABEL_WIDTH}px;
    height: ${c.QUEUE_ROW_HEIGHT}px;
    font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;

    ${({ isFts, problemColor }) =>
        isFts
            ? css`
                  ${createShimmerStyles(problemColor)}
                  border-radius: 4px;
              `
            : css`
                  background-color: ${problemColor || "#4a90e2"};
                  color: ${isShouldUseDarkColor(problemColor)
                      ? "#000"
                      : "#FFF"};
              `}
`;

const QueueRightPart = styled.div`
    height: 100%;
    flex-shrink: 0;
    display: flex;
    flex-wrap: nowrap;
`;

export const QueueRow = ({ runInfo }: { runInfo: QueueRowInfo }) => {
    const scoreboardData = useAppSelector(
        (state) => state.scoreboard[OptimismLevel.normal].ids[runInfo.teamId],
    );
    const teamData = useAppSelector(
        (state) => state.contestInfo.info?.teamsId[runInfo.teamId],
    );
    const probData = useAppSelector(
        (state) => state.contestInfo.info?.problemsId[runInfo.problemId],
    );
    const awards = useAppSelector(
        (state) =>
            state.scoreboard[OptimismLevel.normal].idAwards[runInfo.teamId],
    );
    const rank = useAppSelector(
        (state) =>
            state.scoreboard[OptimismLevel.normal].rankById[runInfo.teamId],
    );
    const medal = awards?.find(
        (award) => award.type == Award.Type.medal,
    ) as Award.medal;

    const isFTSRun =
        (runInfo?.result?.type === "ICPC" &&
            runInfo.result.isFirstToSolveRun) ||
        (runInfo?.result?.type === "IOI" && runInfo.result.isFirstBestRun);

    return (
        <StyledQueueRow>
            <QueueRankLabel rank={rank} medal={medal?.medalColor} />
            <QueueTeamNameLabel
                text={teamData?.shortName ?? "??"}
                fontSize={c.QUEUE_ROW_FONT_SIZE}
            />
            <QueueScoreLabel
                align={"right"}
                text={
                    scoreboardData === null
                        ? "??"
                        : formatScore(scoreboardData?.totalScore ?? 0.0, 1)
                }
            />
            <QueueRightPart>
                <QueueProblemLabel
                    letter={probData?.letter}
                    problemColor={probData?.color}
                    isFts={isFTSRun}
                />
                <QueueRunStatusLabel runInfo={runInfo} />
            </QueueRightPart>
        </StyledQueueRow>
    );
};

export const QueueRowWithTransition: React.FC<{
    row: QueueRowInfo;
    horizontal: boolean;
}> = ({ row, horizontal }) => {
    const [transition, toggle] = useTransition({
        timeout: c.QUEUE_ROW_APPEAR_TIME,
        mountOnEnter: true,
        unmountOnExit: true,
        enter: true,
        exit: true,
    });

    React.useEffect(() => {
        toggle(true);
        return () => toggle(false);
    }, [toggle]);

    if (!transition.isMounted) {
        return null;
    }

    return (
        <QueueRowAnimator
            bottom={row.bottom}
            right={row.right}
            zIndex={row.zIndex}
            fts={row.isFts}
            horizontal={horizontal}
            {...queueRowContractionStates(c.QUEUE_ROW_HEIGHT)[
                transition.status
            ]}
        >
            <QueueRow runInfo={row} />
        </QueueRowAnimator>
    );
};
