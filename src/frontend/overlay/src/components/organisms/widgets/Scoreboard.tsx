import React, {
    useEffect,
    useState,
    useRef,
    useMemo,
    useLayoutEffect,
    useCallback,
    startTransition,
} from "react";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { TaskResultLabel, RankLabel } from "../../atoms/ContestLabels";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";

import {
    formatScore,
    useFormatPenalty,
    useNeedPenalty,
} from "@/services/displayUtils";
import { useResizeObserver } from "usehooks-ts";
import { useAppSelector } from "@/redux/hooks";
import {
    Award,
    ScoreboardSettings,
    OptimismLevel,
    Widget,
    ScoreboardScrollDirection,
    ScoreboardRow as APIScoreboardRow,
    ContestInfo,
    TeamInfo,
    ProblemInfo,
} from "@shared/api";
import { OverlayWidgetC } from "@/components/organisms/widgets/types";

const ScoreboardWrap = styled.div`
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: ${c.SCOREBOARD_GAP};

    box-sizing: border-box;
    width: 100%;
    height: 100%;
    padding: ${c.SCOREBOARD_PADDING_TOP} ${c.SCOREBOARD_PADDING_RIGHT} 0
        ${c.SCOREBOARD_PADDING_LEFT};

    color: ${c.SCOREBOARD_TEXT_COLOR};

    background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
    border-radius: ${c.SCOREBOARD_BORDER_RADIUS};
`;

const ScoreboardHeader = styled.div`
    display: flex;
    flex-direction: row;

    width: 100%;
    padding-top: ${c.SCOREBOARD_HEADER_PADDING_TOP};

    font-size: ${c.SCOREBOARD_CAPTION_FONT_SIZE};
    font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    font-style: normal;
`;

const ScoreboardTitle = styled.div`
    flex: 1 0 0;
`;

const ScoreboardCaption = styled.div``;

const ScoreboardContent = styled.div`
    display: flex;
    flex: 1 0 0;
    flex-direction: column;
    gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;
`;

export const nameTable = {
    normal: c.SCOREBOARD_NORMAL_NAME,
    optimistic: c.SCOREBOARD_OPTIMISTIC_NAME,
    pessimistic: c.SCOREBOARD_PESSIMISTIC_NAME,
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

interface ScoreboardRowProps {
    scoreboardRow: APIScoreboardRow;
    awards: Award[] | null | undefined;
    rank: number;
    teamId: string;
    contestData: ContestInfo & {
        teamsId: Record<TeamInfo["id"], TeamInfo>;
        problemsId: Record<ProblemInfo["id"], ProblemInfo>;
    };
}

const ScoreboardTeamRow = React.memo(
    ({
        scoreboardRow,
        teamId,
        needPenalty,
        contestData,
    }: {
        scoreboardRow: APIScoreboardRow;
        teamId: string;
        needPenalty: boolean;
        contestData: ContestInfo & {
            teamsId: Record<TeamInfo["id"], TeamInfo>;
            problemsId: Record<ProblemInfo["id"], ProblemInfo>;
        };
    }) => {
        const teamData = contestData?.teamsId[teamId];
        const formatPenalty = useFormatPenalty();

        return (
            <>
                <ScoreboardRowName
                    align={c.SCOREBOARD_CELL_TEAMNANE_ALIGN}
                    text={teamData?.shortName ?? "??"}
                />
                <ShrinkingBox
                    align={c.SCOREBOARD_CELL_POINTS_ALIGN}
                    text={
                        scoreboardRow === null
                            ? "??"
                            : formatScore(scoreboardRow?.totalScore ?? 0.0, 1)
                    }
                />
                {needPenalty && (
                    <ShrinkingBox
                        align={c.SCOREBOARD_CELL_PENALTY_ALIGN}
                        text={formatPenalty(scoreboardRow?.penalty)}
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
);

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

const PositionedScoreboardRowDiv = styled.div`
    position: absolute;
    top: 0;
    right: 0;
    left: 0;

    width: 100%;
    height: ${c.SCOREBOARD_ROW_HEIGHT}px;

    will-change: transform;
`;

const ScoreboardRowsWrap = styled.div<{ maxHeight: number }>`
    position: relative;

    overflow: hidden;
    flex: 1 0 0;

    height: auto;
    max-height: ${({ maxHeight }) => `${maxHeight}px`};
`;

export const useScoreboardRows = (
    optimismLevel: OptimismLevel,
    selectedGroup: string,
) => {
    const order = useAppSelector(
        (state) => state.scoreboard[optimismLevel]?.orderById,
    );
    const teamsId = useAppSelector((state) => state.contestInfo.info?.teamsId);

    return useMemo(() => {
        if (teamsId === undefined || order === undefined) {
            return [];
        }
        const result = Object.entries(order).filter(
            ([k]) =>
                selectedGroup === "all" ||
                (teamsId[k]?.groups ?? []).includes(selectedGroup),
        );
        if (selectedGroup !== "all") {
            const rowsNumbers = result.map(([_, b]) => b);
            rowsNumbers.sort((a, b) => (a > b ? 1 : a == b ? 0 : -1));
            const mapping = new Map();
            for (let i = 0; i < rowsNumbers.length; i++) {
                mapping.set(rowsNumbers[i], i);
            }
            for (let i = 0; i < result.length; i++) {
                result[i][1] = mapping.get(result[i][1]);
            }
        }
        return result;
    }, [order, teamsId, selectedGroup]);
};

export const useScroller = (
    totalRows: number,
    singleScreenRowCount: number,
    interval: number,
    direction: ScoreboardScrollDirection | undefined,
) => {
    const effectiveRowCount = Math.max(1, singleScreenRowCount);
    const showRows = totalRows;
    const numPages = Math.max(1, Math.ceil(showRows / effectiveRowCount));
    const singlePageRowCount = Math.ceil(showRows / numPages);

    const curPageRef = useRef(0);
    const [scrollPos, setScrollPos] = useState(() => {
        const pageEndRow = Math.min(
            (curPageRef.current + 1) * singlePageRowCount,
            totalRows,
        );
        return Math.max(0, pageEndRow - effectiveRowCount);
    });

    const calcScrollPos = useCallback(
        (page: number) => {
            const pageEndRow = Math.min(
                (page + 1) * singlePageRowCount,
                totalRows,
            );
            return Math.max(0, pageEndRow - effectiveRowCount);
        },
        [singlePageRowCount, totalRows, effectiveRowCount],
    );

    useEffect(() => {
        if (direction === ScoreboardScrollDirection.FirstPage) {
            curPageRef.current = 0;
            startTransition(() => setScrollPos(calcScrollPos(0)));
        } else if (direction === ScoreboardScrollDirection.LastPage) {
            curPageRef.current = numPages - 1;
            startTransition(() => setScrollPos(calcScrollPos(numPages - 1)));
        }
    }, [direction, numPages, calcScrollPos]);

    useEffect(() => {
        if (
            direction !== ScoreboardScrollDirection.Pause &&
            direction !== ScoreboardScrollDirection.FirstPage &&
            direction !== ScoreboardScrollDirection.LastPage
        ) {
            const intervalId = setInterval(() => {
                const delta =
                    direction === ScoreboardScrollDirection.Back ? -1 : 1;
                let nextPage = curPageRef.current + delta;
                if (nextPage < 0) {
                    nextPage = numPages - 1;
                }
                if (nextPage >= numPages) {
                    nextPage = 0;
                }
                curPageRef.current = nextPage;
                startTransition(() => setScrollPos(calcScrollPos(nextPage)));
            }, interval);
            return () => {
                clearInterval(intervalId);
            };
        }
    }, [interval, numPages, direction, calcScrollPos]);

    return scrollPos;
};

type AnimatingTeam = {
    fromPos: number;
    toPos: number;
    startTime: number;
};

const useAnimatingTeams = (rows: [string, number][]) => {
    const [animatingTeams, setAnimatingTeams] = useState<
        Map<string, AnimatingTeam>
    >(new Map());
    const prevOrderRef = useRef<Map<string, number>>(new Map());

    useEffect(() => {
        const prevOrder = prevOrderRef.current;
        const newAnimating = new Map<string, AnimatingTeam>();

        for (const [teamId, newPos] of rows) {
            const oldPos = prevOrder.get(teamId);
            if (oldPos !== undefined && oldPos !== newPos) {
                newAnimating.set(teamId, {
                    fromPos: oldPos,
                    toPos: newPos,
                    startTime: performance.now(),
                });
            }
        }

        if (newAnimating.size > 0) {
            startTransition(() => {
                setAnimatingTeams((prev) => {
                    const merged = new Map(prev);
                    for (const [k, v] of newAnimating) {
                        merged.set(k, v);
                    }
                    return merged;
                });
            });
        }

        prevOrderRef.current = new Map(rows);
    }, [rows]);

    useEffect(() => {
        if (animatingTeams.size === 0) return;

        const timeout = setTimeout(() => {
            const now = performance.now();
            startTransition(() => {
                setAnimatingTeams((prev) => {
                    const filtered = new Map<string, AnimatingTeam>();
                    for (const [k, v] of prev) {
                        if (
                            now - v.startTime <
                            c.SCOREBOARD_ROW_TRANSITION_TIME
                        ) {
                            filtered.set(k, v);
                        }
                    }
                    return filtered;
                });
            });
        }, c.SCOREBOARD_ROW_TRANSITION_TIME);

        return () => clearTimeout(timeout);
    }, [animatingTeams]);

    return animatingTeams;
};

const useAnimatedScrollPos = (targetScrollPos: number) => {
    const scrollPosRef = useRef(targetScrollPos);
    const animationRef = useRef<number | null>(null);
    const startTimeRef = useRef<number>(0);
    const startPosRef = useRef<number>(targetScrollPos);
    const targetPosRef = useRef<number>(targetScrollPos);
    const subscribersRef = useRef<Set<() => void>>(new Set());

    const subscribe = useCallback((callback: () => void) => {
        subscribersRef.current.add(callback);
        return () => subscribersRef.current.delete(callback);
    }, []);

    const getScrollPos = useCallback(() => scrollPosRef.current, []);

    useLayoutEffect(() => {
        if (targetScrollPos === targetPosRef.current) return;

        startPosRef.current = scrollPosRef.current;
        targetPosRef.current = targetScrollPos;
        startTimeRef.current = performance.now();

        const animate = (now: number) => {
            const elapsed = now - startTimeRef.current;
            const duration = c.SCOREBOARD_ROW_TRANSITION_TIME;
            const progress = Math.min(elapsed / duration, 1);

            const easeInOut =
                progress < 0.5
                    ? 2 * progress * progress
                    : 1 - Math.pow(-2 * progress + 2, 2) / 2;

            scrollPosRef.current =
                startPosRef.current +
                (targetPosRef.current - startPosRef.current) * easeInOut;

            subscribersRef.current.forEach((cb) => cb());

            if (progress < 1) {
                animationRef.current = requestAnimationFrame(animate);
            }
        };

        if (animationRef.current) {
            cancelAnimationFrame(animationRef.current);
        }
        animationRef.current = requestAnimationFrame(animate);

        return () => {
            if (animationRef.current) {
                cancelAnimationFrame(animationRef.current);
            }
        };
    }, [targetScrollPos]);

    return { getScrollPos, subscribe };
};

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
    contestData: ContestInfo & {
        teamsId: Record<TeamInfo["id"], TeamInfo>;
        problemsId: Record<ProblemInfo["id"], ProblemInfo>;
    };
}

const calcCurrentAnimatedPos = (
    animatingInfo: AnimatingTeam | undefined,
    targetPos: number,
): number => {
    if (!animatingInfo) return targetPos;

    const elapsed = performance.now() - animatingInfo.startTime;
    const duration = c.SCOREBOARD_ROW_TRANSITION_TIME;
    const progress = Math.min(elapsed / duration, 1);

    const easeInOut =
        progress < 0.5
            ? 2 * progress * progress
            : 1 - Math.pow(-2 * progress + 2, 2) / 2;

    return (
        animatingInfo.fromPos + (targetPos - animatingInfo.fromPos) * easeInOut
    );
};

const AnimatedRow = React.memo(
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
        rowHeightRef.current = rowHeight;

        const updateTransform = useCallback(() => {
            if (rowRef.current) {
                const visualPos =
                    (animatedPosRef.current - getScrollPos()) *
                        rowHeightRef.current -
                    c.SCOREBOARD_ROW_PADDING;
                rowRef.current.style.transform = `translate3d(0, ${visualPos}px, 0)`;
            }
        }, [getScrollPos]);

        useEffect(() => {
            return subscribeScroll(updateTransform);
        }, [subscribeScroll, updateTransform]);

        useEffect(() => {
            if (animatingInfo) {
                const animate = (now: number) => {
                    const elapsed = now - animatingInfo.startTime;
                    const duration = c.SCOREBOARD_ROW_TRANSITION_TIME;
                    const progress = Math.min(elapsed / duration, 1);

                    const easeInOut =
                        progress < 0.5
                            ? 2 * progress * progress
                            : 1 - Math.pow(-2 * progress + 2, 2) / 2;

                    animatedPosRef.current =
                        animatingInfo.fromPos +
                        (targetPos - animatingInfo.fromPos) * easeInOut;
                    updateTransform();

                    if (progress < 1) {
                        animationRef.current = requestAnimationFrame(animate);
                    }
                };

                if (animationRef.current) {
                    cancelAnimationFrame(animationRef.current);
                }
                animationRef.current = requestAnimationFrame(animate);

                return () => {
                    if (animationRef.current) {
                        cancelAnimationFrame(animationRef.current);
                    }
                };
            } else {
                animatedPosRef.current = targetPos;
                updateTransform();
            }
        }, [targetPos, animatingInfo, updateTransform]);

        const initialVisualPos =
            (animatedPosRef.current - getScrollPos()) * rowHeight -
            c.SCOREBOARD_ROW_PADDING;

        return (
            <PositionedScoreboardRowDiv
                ref={rowRef}
                style={{
                    zIndex,
                    transform: `translate3d(0, ${initialVisualPos}px, 0)`,
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
);

interface ScoreboardRowsProps {
    settings: ScoreboardSettings;
    onPage: number;
}

export const ScoreboardRows = ({ settings, onPage }: ScoreboardRowsProps) => {
    const rows = useScoreboardRows(settings.optimismLevel, settings.group);
    const rowHeight = c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING;
    const targetScrollPos = useScroller(
        rows.length,
        onPage,
        c.SCOREBOARD_SCROLL_INTERVAL,
        settings.scrollDirection,
    );
    const { getScrollPos, subscribe } = useAnimatedScrollPos(targetScrollPos);
    const scoreboardData = useAppSelector(
        (state) => state.scoreboard[settings.optimismLevel],
    );
    const normalScoreboardData = useAppSelector(
        (state) => state.scoreboard[OptimismLevel.normal],
    );
    const contestData = useAppSelector((state) => state.contestInfo.info);

    const animatingTeams = useAnimatingTeams(rows);

    const [prevWindow, setPrevWindow] = useState({
        scrollPos: targetScrollPos,
        onPage,
    });

    const visibleTeams = useMemo(() => {
        const effectiveOnPage = Math.max(1, onPage);
        const effectivePrevOnPage = Math.max(1, prevWindow.onPage);

        const currentMin = targetScrollPos;
        const currentMax = targetScrollPos + effectiveOnPage;

        const prevMin = prevWindow.scrollPos;
        const prevMax = prevWindow.scrollPos + effectivePrevOnPage;

        const visible = new Set<string>();

        for (const [teamId, position] of rows) {
            if (position >= currentMin && position <= currentMax) {
                visible.add(teamId);
            }
            if (position >= prevMin && position <= prevMax) {
                visible.add(teamId);
            }
        }

        for (const [teamId, info] of animatingTeams) {
            const trajMin = Math.min(info.fromPos, info.toPos);
            const trajMax = Math.max(info.fromPos, info.toPos);

            if (trajMax >= currentMin && trajMin <= currentMax) {
                visible.add(teamId);
            }
        }

        return visible;
    }, [rows, targetScrollPos, onPage, animatingTeams, prevWindow]);

    useEffect(() => {
        const timeout = setTimeout(() => {
            startTransition(() =>
                setPrevWindow({ scrollPos: targetScrollPos, onPage }),
            );
        }, c.SCOREBOARD_ROW_TRANSITION_TIME);
        return () => clearTimeout(timeout);
    }, [targetScrollPos, onPage]);

    const teamsToRender = useMemo(() => {
        return rows.filter(([teamId]) => visibleTeams.has(teamId));
    }, [rows, visibleTeams]);

    const effectiveOnPage = Math.max(1, onPage);

    return (
        <ScoreboardRowsWrap maxHeight={effectiveOnPage * rowHeight}>
            {teamsToRender.map(([teamId, position]) => (
                <AnimatedRow
                    key={teamId}
                    teamId={teamId}
                    targetPos={position}
                    animatingInfo={animatingTeams.get(teamId)}
                    rowHeight={rowHeight}
                    getScrollPos={getScrollPos}
                    subscribeScroll={subscribe}
                    zIndex={rows.length - position}
                    scoreboardRow={scoreboardData?.ids[teamId]}
                    rank={normalScoreboardData?.rankById[teamId]}
                    awards={scoreboardData?.idAwards[teamId]}
                    contestData={contestData}
                />
            ))}
        </ScoreboardRowsWrap>
    );
};

const ScoreboardTableHeaderWrap = styled(ScoreboardTableRowWrap)`
    overflow: hidden;

    height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

    font-size: ${c.SCOREBOARD_HEADER_FONT_SIZE};
    font-weight: ${c.SCOREBOARD_HEADER_FONT_WEIGHT};
    font-style: normal;
    line-height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

    border-radius: ${c.SCOREBOARD_HEADER_BORDER_RADIUS_TOP_LEFT}
        ${c.SCOREBOARD_HEADER_BORDER_RADIUS_TOP_RIGHT} 0 0;
`;

const ScoreboardTableHeaderCell = styled.div`
    padding: 0 ${c.SCOREBOARD_CELL_PADDING};
    text-align: center;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    background-color: ${c.SCOREBOARD_HEADER_BACKGROUND_COLOR};
`;

const ScoreboardTableHeaderNameCell = styled(ScoreboardTableHeaderCell)`
    text-align: left;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
`;

const ScoreboardProblemLabel = styled(ProblemLabel)`
    width: unset;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
`;

const ScoreboardTableHeader = () => {
    const problems = useAppSelector(
        (state) => state.contestInfo.info?.problems,
    );
    const needPenalty = useNeedPenalty();
    return (
        <ScoreboardTableHeaderWrap
            nProblems={Math.max(problems?.length ?? 0, 1)}
            needPenalty={needPenalty}
        >
            <ScoreboardTableHeaderCell>#</ScoreboardTableHeaderCell>
            <ScoreboardTableHeaderNameCell>Name</ScoreboardTableHeaderNameCell>
            <ScoreboardTableHeaderCell>Σ</ScoreboardTableHeaderCell>
            {needPenalty && (
                <ScoreboardTableHeaderCell>
                    <ShrinkingBox text={"Penalty"} />
                </ScoreboardTableHeaderCell>
            )}
            {problems &&
                problems.map((probData) => (
                    <ScoreboardProblemLabel
                        key={probData.name}
                        letter={probData.letter}
                        problemColor={probData.color}
                    />
                ))}
        </ScoreboardTableHeaderWrap>
    );
};

export const Scoreboard: OverlayWidgetC<Widget.ScoreboardWidget> = ({
    widgetData: { settings },
}) => {
    const ref = useRef<HTMLDivElement>(null);
    const { height = 0 } = useResizeObserver({ ref });
    const onPage = Math.floor(
        (height - c.SCOREBOARD_HEADER_HEIGHT) /
            (c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING),
    );

    return (
        <ScoreboardWrap>
            <ScoreboardHeader>
                <ScoreboardTitle>
                    {nameTable[settings.optimismLevel] ??
                        c.SCOREBOARD_UNDEFINED_NAME}{" "}
                    {c.SCOREBOARD_STANDINGS_NAME}
                </ScoreboardTitle>
                <ScoreboardCaption>{c.SCOREBOARD_CAPTION}</ScoreboardCaption>
            </ScoreboardHeader>
            <ScoreboardContent ref={ref}>
                <ScoreboardTableHeader />
                <ScoreboardRows settings={settings} onPage={onPage} />
            </ScoreboardContent>
        </ScoreboardWrap>
    );
};

export default Scoreboard;
