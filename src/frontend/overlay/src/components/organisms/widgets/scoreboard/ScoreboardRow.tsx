import React, { useRef, useEffect, useCallback, useLayoutEffect } from "react";
import styled from "styled-components";
import c from "@/config";
import { TaskResultLabel, RankLabel } from "@/components/atoms/ContestLabels";
import { ShrinkingBox } from "../../../atoms/ShrinkingBox";
import {
    formatScore,
    useFormatPenalty,
    useNeedPenalty,
} from "@/services/displayUtils";
import {
    Award,
    ScoreboardRow as APIScoreboardRow,
    ContestInfo,
    TeamInfo,
    ProblemInfo,
} from "@shared/api";
import { AnimatingTeam } from "@/components/organisms/widgets/scoreboard/hooks/useScoreboardAnimation";
import { calculateProgress, interpolate } from "@/components/organisms/widgets/scoreboard/utils/easingFunctions";

type ContestDataWithMaps = ContestInfo & {
    teamsId: Record<TeamInfo["id"], TeamInfo>;
    problemsId: Record<ProblemInfo["id"], ProblemInfo>;
};

const ScoreboardTableRowWrap = styled.div<{
    needPenalty: boolean;
    nProblems: number;
}>`
    display: grid;
    grid-template-columns:
        ${c.SCOREBOARD_CELL_PLACE_SIZE}
        ${c.SCOREBOARD_CELL_TEAMNAME_SIZE}
        ${c.SCOREBOARD_CELL_POINTS_SIZE}
        ${({ needPenalty }) =>
            needPenalty ? c.SCOREBOARD_CELL_PENALTY_SIZE : ""}
        repeat(${(props) => props.nProblems}, 1fr);
    gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;

    box-sizing: border-box;

    background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
`;


const ScoreboardRowWrap = styled(ScoreboardTableRowWrap)`
    overflow: hidden;
    align-items: center;

    box-sizing: content-box;
    height: ${c.SCOREBOARD_ROW_HEIGHT}px;

    font-size: ${c.SCOREBOARD_ROW_FONT_SIZE};
    font-weight: ${c.SCOREBOARD_TABLE_ROW_FONT_WEIGHT};
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    font-style: normal;

    border-top: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
    border-bottom: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
`;

const ScoreboardRowName = styled(ShrinkingBox)`
    padding: 0 ${c.SCOREBOARD_CELL_PADDING};
`;

const ScoreboardRankLabel = styled(RankLabel)`
    display: flex;
    align-items: center;
    align-self: stretch;
    justify-content: center;
`;

export const ScoreboardTaskResultLabel = styled(TaskResultLabel)`
    display: flex;
    align-items: center;
    align-self: stretch;
    justify-content: center;
    position: relative;
    overflow: hidden;
`;

const PositionedScoreboardRowDiv = styled.div`
    position: absolute;
    top: 0;
    right: 0;
    left: 0;

    width: 100%;
    height: ${c.SCOREBOARD_ROW_HEIGHT}px;

    will-change: transform;

    /* Performance optimization: isolate layout calculations */
    contain: layout style paint;
`;

interface ScoreboardTeamRowProps {
    scoreboardRow: APIScoreboardRow;
    teamId: string;
    needPenalty: boolean;
    contestData: ContestDataWithMaps;
}

const ScoreboardTeamRow = React.memo(
    ({
        scoreboardRow,
        teamId,
        needPenalty,
        contestData,
    }: ScoreboardTeamRowProps) => {
        const teamData = contestData?.teamsId[teamId];
        const formatPenalty = useFormatPenalty();

        const teamName = teamData?.shortName ?? "??";
        const scoreText = scoreboardRow === null
            ? "??"
            : formatScore(scoreboardRow?.totalScore ?? 0.0, 1);
        const penaltyText = formatPenalty(scoreboardRow?.penalty);

        return (
            <>
                <ScoreboardRowName
                    align={c.SCOREBOARD_CELL_TEAMNANE_ALIGN}
                    text={teamName}
                />
                <ShrinkingBox
                    align={c.SCOREBOARD_CELL_POINTS_ALIGN}
                    text={scoreText}
                />
                {needPenalty && (
                    <ShrinkingBox
                        align={c.SCOREBOARD_CELL_PENALTY_ALIGN}
                        text={penaltyText}
                    />
                )}
                {scoreboardRow?.problemResults.map((result, i) => (
                    <ScoreboardTaskResultLabel
                        problemResult={result}
                        key={i}
                        problemColor={contestData?.problems[i]?.color}
                        minScore={contestData?.problems[i]?.minScore}
                        maxScore={contestData?.problems[i]?.maxScore}
                    />
                ))}
            </>
        );
    },
    (prevProps: ScoreboardTeamRowProps, nextProps: ScoreboardTeamRowProps) => {
        return (
            prevProps.teamId === nextProps.teamId &&
            prevProps.scoreboardRow === nextProps.scoreboardRow &&
            prevProps.needPenalty === nextProps.needPenalty &&
            prevProps.contestData === nextProps.contestData
        );
    }
);
ScoreboardTeamRow.displayName = "ScoreboardTeamRow";

interface ScoreboardRowProps {
    scoreboardRow: APIScoreboardRow;
    awards: Award[] | null | undefined;
    rank: number;
    teamId: string;
    contestData: ContestDataWithMaps;
}

export const ScoreboardRow = React.memo(
    ({
        scoreboardRow,
        awards,
        rank,
        teamId,
        contestData,
    }: ScoreboardRowProps) => {
        const medal = awards?.find(
            (award) => award.type == Award.Type.medal,
        ) as Award.medal;
        const needPenalty = useNeedPenalty();
        
        return (
            <ScoreboardRowWrap
                nProblems={Math.max(contestData?.problems?.length ?? 0, 1)}
                needPenalty={needPenalty}
            >
                <ScoreboardRankLabel rank={rank} medal={medal?.medalColor} />
                <ScoreboardTeamRow
                    scoreboardRow={scoreboardRow}
                    teamId={teamId}
                    needPenalty={needPenalty}
                    contestData={contestData}
                />
            </ScoreboardRowWrap>
        );
    },
);

interface AnimatedRowProps {
    teamId: string;
    targetPos: number;
    animatingInfo: AnimatingTeam | undefined;
    rowHeight: number;
    getScrollPos: () => number;
    subscribeScroll: (cb: () => void) => () => void;
    zIndex: number;
    scoreboardRow: APIScoreboardRow;
    rank: number;
    awards: Award[] | null | undefined;
    contestData: ContestDataWithMaps;
}

function calcCurrentAnimatedPos(
    animatingInfo: AnimatingTeam | undefined,
    targetPos: number,
): number {
    if (!animatingInfo) return targetPos;

    const progress = calculateProgress(
        animatingInfo.startTime,
        c.SCOREBOARD_ROW_TRANSITION_TIME,
    );

    return interpolate(animatingInfo.fromPos, targetPos, progress);
};

export const AnimatedRow = React.memo(
    ({
        teamId,
        targetPos,
        animatingInfo,
        rowHeight,
        getScrollPos,
        subscribeScroll,
        zIndex,
        scoreboardRow,
        rank,
        awards,
        contestData,
    }: AnimatedRowProps) => {
        const rowRef = useRef<HTMLDivElement>(null);
        const animatedPosRef = useRef<number>(
            calcCurrentAnimatedPos(animatingInfo, targetPos),
        );
        const animationRef = useRef<number | null>(null);
        const rowHeightRef = useRef(rowHeight);
        const getScrollPosRef = useRef(getScrollPos);
        const lastVisualPosRef = useRef<number | null>(null);

        useLayoutEffect(() => {
            rowHeightRef.current = rowHeight;
            getScrollPosRef.current = getScrollPos;
        });

        const updateTransform = useCallback(() => {
            if (rowRef.current) {
                const visualPos =
                    (animatedPosRef.current - getScrollPosRef.current()) *
                        rowHeightRef.current -
                    c.SCOREBOARD_ROW_PADDING;

                const roundedPos = Math.round(visualPos);

                if (lastVisualPosRef.current !== roundedPos) {
                    lastVisualPosRef.current = roundedPos;
                    rowRef.current.style.transform = `translate3d(0, ${roundedPos}px, 0)`;
                }
            }
        }, []);

        useEffect(() => {
            return subscribeScroll(updateTransform);
        }, [subscribeScroll, updateTransform]);

        useEffect(() => {
            if (animationRef.current) {
                cancelAnimationFrame(animationRef.current);
                animationRef.current = null;
            }

            if (animatingInfo) {
                const animate = (_now: number) => {
                    const progress = calculateProgress(
                        animatingInfo.startTime,
                        c.SCOREBOARD_ROW_TRANSITION_TIME,
                    );

                    animatedPosRef.current = interpolate(
                        animatingInfo.fromPos,
                        targetPos,
                        progress,
                    );
                    updateTransform();

                    if (progress < 1) {
                        animationRef.current = requestAnimationFrame(animate);
                    } else {
                        animatedPosRef.current = targetPos;
                        updateTransform();
                    }
                };

                animationRef.current = requestAnimationFrame(animate);

                return () => {
                    if (animationRef.current) {
                        cancelAnimationFrame(animationRef.current);
                        animationRef.current = null;
                    }
                };
            } else {
                const currentPos = animatedPosRef.current;
                if (Math.abs(currentPos - targetPos) > 0.01) {
                    const startPos = currentPos;
                    const startTime = performance.now();

                    const smoothTransition = (now: number) => {
                        const elapsed = now - startTime;
                        const progress = Math.min(elapsed / c.SCOREBOARD_ROW_TRANSITION_TIME, 1);

                        animatedPosRef.current = interpolate(startPos, targetPos, progress);
                        updateTransform();

                        if (progress < 1) {
                            animationRef.current = requestAnimationFrame(smoothTransition);
                        }
                    };

                    animationRef.current = requestAnimationFrame(smoothTransition);

                    return () => {
                        if (animationRef.current) {
                            cancelAnimationFrame(animationRef.current);
                            animationRef.current = null;
                        }
                    };
                } else {
                    animatedPosRef.current = targetPos;
                    updateTransform();
                }
            }
        }, [targetPos, animatingInfo, updateTransform]);

        useLayoutEffect(() => {
            updateTransform();
        }, [updateTransform]);

        return (
            <PositionedScoreboardRowDiv
                ref={rowRef}
                style={{
                    zIndex,
                }}
            >
                <ScoreboardRow
                    scoreboardRow={scoreboardRow}
                    rank={rank}
                    awards={awards}
                    teamId={teamId}
                    contestData={contestData}
                />
            </PositionedScoreboardRowDiv>
        );
    },
    (prevProps: AnimatedRowProps, nextProps: AnimatedRowProps) => {
        return (
            prevProps.teamId === nextProps.teamId &&
            prevProps.targetPos === nextProps.targetPos &&
            prevProps.animatingInfo === nextProps.animatingInfo &&
            prevProps.rowHeight === nextProps.rowHeight &&
            prevProps.zIndex === nextProps.zIndex &&
            prevProps.scoreboardRow === nextProps.scoreboardRow &&
            prevProps.rank === nextProps.rank &&
            prevProps.awards === nextProps.awards &&
            prevProps.contestData === nextProps.contestData &&
            prevProps.getScrollPos === nextProps.getScrollPos &&
            prevProps.subscribeScroll === nextProps.subscribeScroll
        );
    }
);
AnimatedRow.displayName = "AnimatedRow";
