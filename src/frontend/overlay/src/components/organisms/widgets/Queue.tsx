import { useCallback, useMemo, useRef, useState } from "react";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { css, CSSObject, Keyframes, keyframes } from "styled-components";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { RankLabel, RunStatusLabel } from "../../atoms/ContestLabels";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { useDelayedBoolean } from "@/utils/hooks/withTimeoutAfterRender";
import star from "../../../assets/icons/star.svg";
import star_mask from "../../../assets/icons/star_mask.svg";
import { formatScore } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";
import { Award, OptimismLevel, RunInfo, Widget } from "@shared/api";
import { isFTS } from "@/utils/statusInfo";
import { TeamMediaHolder } from "@/components/organisms/holder/TeamMediaHolder";

// const MAX_QUEUE_ROWS_COUNT = 20;

// Needed just for positioning and transitions. Don't use for anything else
interface QueueRowAnimatorProps {
    bottom: number,
    right: number,
    horizontal?: boolean,
    zIndex: number,
    animation: Keyframes,
    fts: boolean
}
const QueueRowAnimator = styled.div.attrs<QueueRowAnimatorProps>(({ bottom, right, zIndex }) => {
    return ({
        style: {
            bottom: bottom + "px",
            right: right + "px",
            zIndex: zIndex,
        }
    });
})<QueueRowAnimatorProps>`
  overflow: hidden;
  width: ${({ horizontal }) => horizontal ? (c.QUEUE_ROW_WIDTH + "px") : "100%"};

  position: absolute;
  transition-duration: ${({ fts }) => fts ? c.QUEUE_ROW_FTS_TRANSITION_TIME : c.QUEUE_ROW_TRANSITION_TIME}ms;
  transition-timing-function: linear;
  animation: ${({ animation }) => animation} ${c.QUEUE_ROW_APPEAR_TIME}ms linear; /* dissapear is also linear for now. FIXME */
  animation-fill-mode: forwards;
`;

const rowExpand = (fullHeight) => keyframes`
  from {
    max-height: 0;
  }

  to {
    max-height: ${fullHeight}px;
  }
`;

const slideOutToRight = () => keyframes`
  from {
    transform: translate(0, 0);
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
  to {
    transform: translate(100%, 0);
    opacity: 0;
  }
`;

const slideInFromRight = () => keyframes`
  from {
    transform: translate(100%, 0);
  }
  to {
    transform: translate(0, 0);
  }
`;

const fadeOut = () => keyframes`
  from {
    opacity: 100%;
  }
  to {
    opacity: 0;
  }
`;

const appearStatesFeatured = {
    // entering: {},
    entering: css`
      animation: ${slideInFromRight()} ${c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-out;
    `,
    exiting: css`
      animation: ${slideOutToRight()} ${c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-in;
    `,
    exited: css`
      opacity: 0
    `,
};

const queueRowContractionStates = (fullHeight) => ({
    entering: {
        animation: rowExpand(fullHeight),
        style: { alignItems: "flex-start" },
    },
    entered: {},
    exiting: {
        animation: fadeOut(),
        // animation: slideOutToRight(fullHeight),
        // style: {alignItems: "flex-start"},
    },
    exited: {},
});

interface QueueRowInfo extends RunInfo {
    // isEven: boolean,
    zIndex: number,
    bottom: number,
    right: number,
    isFeatured: boolean,
    isFeaturedRunMediaLoaded: boolean,
    isFts: boolean,
    setIsFeaturedRunMediaLoaded: (state: boolean) => void | null,
}

type QueueBatchInfo = { [runId: string]: number; };
type DelegateId = string;
type QueueState = {
    currentRuns: { [runId: string]: DelegateId }; // runId => batchDelegateId
    batches: { [delegateId: DelegateId]: QueueBatchInfo };
    batchOrder: DelegateId[]; // list of deligate ids from newest to oldest
    ftsPositions: { [runId: string]: number };
};

const useVerticalQueueRowsData = ({
    width,
    height,
    basicZIndex = c.QUEUE_BASIC_ZINDEX
}: {
    width: number,
    height: number,
    basicZIndex?: number,
    horizontal?: boolean,
}): [QueueRowInfo | null, QueueRowInfo[]] => {
    const bottomPosition = (index: number) => (c.QUEUE_ROW_HEIGHT + c.QUEUE_ROW_Y_PADDING) * index;
    const allowedMaxRows = Math.min((width / c.QUEUE_ROW_WIDTH) * (height / c.QUEUE_ROW_HEIGHT), c.QUEUE_MAX_ROWS);

    const { queue, totalQueueItems } = useAppSelector(state => state.queue);

    const [loadedMediaRun, setLoadedMediaRun] = useState(null);

    let rows: QueueRowInfo[] = [];
    let featured: QueueRowInfo | null = null;
    let totalFts = 0;
    queue.forEach((run, runIndex) => {
        const row: QueueRowInfo = {
            ...run,
            // isEven: (totalQueueItems - runIndex) % 2 === 0,
            zIndex: basicZIndex - runIndex + totalQueueItems,
            bottom: 0,
            right: 0,
            isFeatured: false,
            isFeaturedRunMediaLoaded: false,
            isFts: isFTS(run),
            setIsFeaturedRunMediaLoaded: null,
        };
        if (row.isFts) {
            totalFts++;
            row.bottom = height;
        }
        if (run.featuredRunMedia !== undefined) {
            row.isFeatured = true;
            row.isFeaturedRunMediaLoaded = loadedMediaRun === run.id;
            row.setIsFeaturedRunMediaLoaded = (state) => {
                setLoadedMediaRun(state ? run.id : null);
            };
            featured = row;
        } else {
            rows.push(row);
        }
    });
    let ftsRowCount = 0;
    let regularRowCount = 0;
    rows.forEach((row) => {
        if (row.isFts) {
            row.bottom = height - bottomPosition(totalFts - ftsRowCount) + 3;
            ftsRowCount++;
        } else  {
            row.bottom = bottomPosition(regularRowCount);
            regularRowCount++;
        }
    });
    const allowedRegular = allowedMaxRows - ftsRowCount;
    rows = rows.filter((row, index) => {
        return row.isFts || index < allowedRegular;
    });
    return [featured, rows];
};

const horizontalQueueRowGridOffsetX = c.QUEUE_ROW_WIDTH + c.QUEUE_HORIZONTAL_ROW_X_PADDING;
const horizontalQueueRowGridOffsetY = c.QUEUE_ROW_HEIGHT + c.QUEUE_HORIZONTAL_ROW_Y_PADDING;
const useHorizontalQueueRowsData = ({
    width,
    height,
    ftsRowWidth,
    basicZIndex = c.QUEUE_BASIC_ZINDEX,
}: {
    width: number,
    height: number,
    ftsRowWidth: number,
    basicZIndex?: number,
}): [QueueRowInfo | null, QueueRowInfo[]] => {
    const bottomPosition = useCallback((index: number) => {
        return (c.QUEUE_ROW_HEIGHT + c.QUEUE_HORIZONTAL_ROW_Y_PADDING) * index;
    }, []);
    const rightPosition = useCallback((index: number) => {
        return horizontalQueueRowGridOffsetX * index;
    }, []);
    const allowedMaxBatches = useMemo(() => {
        return Math.floor(height / horizontalQueueRowGridOffsetY) - 1;
    }, [width, height]);
    const allowedFts = useMemo(() => {
        return Math.floor(ftsRowWidth / horizontalQueueRowGridOffsetX) * horizontalQueueRowGridOffsetX;
    }, [ftsRowWidth]);

    const { queue, totalQueueItems } = useAppSelector(state => state.queue);
    const [loadedMediaRun, setLoadedMediaRun] = useState(null);

    const queueStateRef = useRef<QueueState>({ currentRuns: {}, batches: {}, batchOrder: [], ftsPositions: {} });
    return useMemo(() => {
        const newState: QueueState = {
            currentRuns: {},
            batches: {},
            batchOrder: [ ...queueStateRef.current.batchOrder ],
            ftsPositions: { ...queueStateRef.current.ftsPositions },
        };
        for (const [dId, bi] of Object.entries(queueStateRef.current.batches)) {
            newState.batches[dId] = { ...bi };
        }
        for (const run of queue) {
            if (run.featuredRunMedia !== undefined || newState.ftsPositions[run.id] !== undefined) {
                continue;
            } else if (isFTS(run)) {
                const usedPositions = Object.values(newState.ftsPositions);
                let selectedPosition: number | null = null;
                for (let i = 0; i < allowedFts; i++) {
                    if (!usedPositions.includes(i)) {
                        selectedPosition = i;
                        break;
                    }
                }
                if (selectedPosition !== null) {
                    console.info(`Make run ${run.id} fts with position ${selectedPosition}`, run);
                    newState.ftsPositions[run.id] = selectedPosition;
                    continue;
                }
            }
            const delegateId = queueStateRef.current.currentRuns[run.id];
            if (delegateId) {
                newState.currentRuns[run.id] = delegateId;
            } else {
                console.info("New run in queue", run);
                if (newState.batchOrder.length === 0) { // no any batch yet
                    newState.batchOrder.splice(0, 0, run.id);
                    newState.batches[run.id] = { [run.id]: 0 };
                } else {
                    const usedPositions = Object.values(newState.batches[newState.batchOrder[0]]);
                    if (usedPositions.length >= c.QUEUE_HORIZONTAL_HEIGHT_NUM) { // allocate new batch
                        newState.batchOrder.splice(0, 0, run.id);
                        newState.batches[run.id] = { [run.id]: 0 };
                    } else { // put in existing butch
                        for (let i = 0; i < c.QUEUE_HORIZONTAL_HEIGHT_NUM; i++) {
                            if (!usedPositions.includes(i)) {
                                newState.batches[newState.batchOrder[0]][run.id] = i;
                                break;
                            }
                        }
                    }
                }
                newState.currentRuns[run.id] = newState.batchOrder[0];
            }
        }
        for (const [runId, dId] of Object.entries(queueStateRef.current.currentRuns)) {
            if (queue.find(r =>
                r.id === runId && r.featuredRunMedia === undefined && newState.ftsPositions[runId] === undefined
            ) == undefined) { // where remove run or make featured or make fts
                delete newState.currentRuns[runId];
                delete newState.batches[dId][runId];
                if (Object.keys(newState.batches[dId]).length === 0) { // remove batch, if it empty
                    newState.batchOrder = newState.batchOrder.filter(bId => bId !== dId);
                }
            }
        }
        for (const runId of Object.keys(newState.ftsPositions)) {
            if (queue.find(r => r.id === runId) === undefined) { // if remove run, but it was fts
                delete newState.ftsPositions[runId];
                console.info("Drop fts run in queue", runId);
            }
        }

        // console.log("NewState", newState);
        queueStateRef.current = newState;

        // console.log("New runs", queue.map(r => r.id).join(","))
        let featured: QueueRowInfo | null = null;
        const rows: QueueRowInfo[] = [];
        queue.forEach((run, runIndex) => {
            const row: QueueRowInfo = {
                ...run,
                zIndex: basicZIndex - runIndex + totalQueueItems,
                bottom: 0,
                right: 0,
                isFeatured: false,
                isFeaturedRunMediaLoaded: false,
                isFts: isFTS(run),
                setIsFeaturedRunMediaLoaded: null,
            };
            if (run.featuredRunMedia !== undefined) {
                row.isFeatured = true;
                row.isFeaturedRunMediaLoaded = loadedMediaRun === run.id;
                row.setIsFeaturedRunMediaLoaded = (state) => {
                    setLoadedMediaRun(state ? run.id : null);
                };
                featured = row;
                return;
            } else if (newState.ftsPositions[run.id] !== undefined) {
                row.bottom = height - c.QUEUE_HORIZONTAL_ROW_Y_PADDING - c.QUEUE_ROW_HEIGHT;
                row.right = rightPosition(newState.ftsPositions[run.id]);
                rows.push(row);
                return;
            }
            const delegateId = newState.currentRuns[run.id];
            const right = newState.batches[delegateId][run.id];
            const bottom = newState.batchOrder.findIndex(id => id == delegateId);
            if (bottom >= allowedMaxBatches) { // skip run, no vertical overflow
                return;
            }
            row.bottom = bottomPosition(bottom);
            row.right = rightPosition(c.QUEUE_HORIZONTAL_HEIGHT_NUM - 1 - right);
            rows.push(row);
        });
        return [featured, rows];
    }, [queue, loadedMediaRun, setLoadedMediaRun, height]);
};

const QueueRankLabel = styled(RankLabel)`
  width: 32px;
  align-self: stretch;
  padding-left: 12px;
  flex-shrink: 0;
`;

const QueueTeamNameLabel = styled(ShrinkingBox)`
  flex-grow: 1;
`;
const QueueRunStatusLabel = styled(RunStatusLabel)`
  width: 46px;
  flex-shrink: 0;
`;

const StyledQueueRow = styled.div`
  width: 100%;
  height: ${c.QUEUE_ROW_HEIGHT}px;
  display: flex;
  align-items: center;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  overflow: hidden;
  gap: 5px;
  color: white;
  font-size: ${c.QUEUE_ROW_FONT_SIZE};
  background: ${c.QUEUE_ROW_BACKGROUND};
`;

const QueueScoreLabel = styled(ShrinkingBox)`
  width: 51px;
  flex-shrink: 0;
  flex-direction: row-reverse;
`;
const QueueProblemLabel = styled(ProblemLabel)`
  width: ${c.QUEUE_ROW_PROBLEM_LABEL_WIDTH}px;
  font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  line-height: ${c.QUEUE_ROW_HEIGHT}px;
  flex-shrink: 0;
  background-image: ${({ isFts }) => isFts ? `url(${star})` : null};
  background-repeat: no-repeat;
  background-position: 50%;
  background-size: contain;

  /*
  These three lines trigger plugin/no-unsupported-browser-features.
  I don't belive it, but we have to check.
   */
    /* stylelint-disable plugin/no-unsupported-browser-features */
  mask: ${({ isFts }) => isFts ? `url(${star_mask}) 50% 50% no-repeat` : null};
  mask-position: 50%;
  mask-size: contain;
    /* stylelint-enable plugin/no-unsupported-browser-features */
`;
const QueueRightPart = styled.div`
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap;
`;

export const QueueRow = ({ runInfo }) => {
    const scoreboardData = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].ids[runInfo.teamId]);
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[runInfo.teamId]);
    const probData = useAppSelector((state) => state.contestInfo.info?.problemsId[runInfo.problemId]);
    const awards = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].idAwards[runInfo.teamId]);
    const rank = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].rankById[runInfo.teamId]);
    const medal = awards?.find((award) => award.type == Award.Type.medal) as Award.medal;
    const isFTSRun = runInfo?.result?.type === "ICPC" && runInfo.result.isFirstToSolveRun || runInfo?.result?.type === "IOI" && runInfo.result.isFirstBestRun;
    return <StyledQueueRow>
        <QueueRankLabel rank={rank} medal={medal?.medalColor}/>
        <QueueTeamNameLabel text={teamData?.shortName ?? "??"}/>
        <QueueScoreLabel align={"right"}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
        />
        <QueueRightPart>
            <QueueProblemLabel letter={probData?.letter} problemColor={probData?.color} isFts={isFTSRun}/>
            <QueueRunStatusLabel runInfo={runInfo}/>
        </QueueRightPart>
    </StyledQueueRow>;
};

const QueueWrap = styled.div<{ hasFeatured: boolean; variant: "vertical" | "horizontal" }>`
  width: 100%;
  height: 100%;
  position: absolute;
  background-color: ${({ variant }) => variant === "horizontal" ? c.QUEUE_HORIZONTAL_BACKGROUND_COLOR : c.QUEUE_BACKGROUND_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  border-top-right-radius: ${props => props.hasFeatured ? "0" : c.GLOBAL_BORDER_RADIUS};
  padding: ${c.QUEUE_WRAP_PADDING}px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 7px;
`;

const RowsContainer = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
`;
const HorizontalRowsContainer = styled.div`
  position: absolute;
  width: calc(100% - ${c.QUEUE_WRAP_PADDING * 2}px);
  height: calc(100% - ${c.QUEUE_WRAP_PADDING * 2}px);
  overflow: hidden;
`;

const QueueHeader = styled.div`
  font-size: ${c.QUEUE_HEADER_FONT_SIZE};
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  line-height: ${c.QUEUE_HEADER_LINE_HEIGHT};
  color: white;
  width: fit-content;
  max-width: 100%;
  display: flex;
`;

const Title = styled.div`
  flex: 1 0 0;
`;

const Caption = styled.div`
`;

const StyledFeatured = styled.div<{additional: CSSObject}>`
  width: 334px;
  position: absolute;

  right: calc(100% - 16px); /* this with padding is a hack to hide the rounded corner of the widget */
  padding: 3px 16px 3px 3px;

  background-color: ${c.QUEUE_BACKGROUND_COLOR};
  border-radius: 16px 0 0 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: ${c.QUEUE_ROW_FEATURED_RUN_PADDING}px;

  ${({ additional }) => additional}
`;

export const Featured = ({ runInfo }: { runInfo: QueueRowInfo }) => {
    return <TransitionGroup component={null}>
        {runInfo && <Transition timeout={c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME} key={runInfo.id}>
            {state => {
                const realState = runInfo.isFeaturedRunMediaLoaded ? state : "exited";
                return (
                    <StyledFeatured additional={appearStatesFeatured[realState]}>
                        <TeamMediaHolder
                            media={runInfo.featuredRunMedia}
                            onLoadStatus={runInfo.setIsFeaturedRunMediaLoaded}
                        />
                        <QueueRow runInfo={runInfo}/>
                    </StyledFeatured>
                );
            }}
        </Transition>
        }
    </TransitionGroup>;
};


const StyledHorizontalFeatured = styled.div<{additional: CSSObject}>`
  width: 334px;
  position: absolute;

  right: 0;
  bottom: 100%;
  padding: ${c.QUEUE_WRAP_PADDING}px;

  background-color: ${c.QUEUE_BACKGROUND_COLOR};
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: ${c.QUEUE_ROW_FEATURED_RUN_PADDING}px;

  ${({ additional }) => additional}
`;

export const HorizontalFeatured = ({ runInfo }: { runInfo: QueueRowInfo }) => {
    return <TransitionGroup component={null}>
        {runInfo && <Transition timeout={c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME} key={runInfo.id}>
            {state => {
                const realState = runInfo.isFeaturedRunMediaLoaded ? state : "exited";
                return (
                    <StyledHorizontalFeatured additional={appearStatesFeatured[realState]}>
                        <TeamMediaHolder
                            media={runInfo.featuredRunMedia}
                            onLoadStatus={runInfo.setIsFeaturedRunMediaLoaded}
                        />
                        <QueueRow runInfo={runInfo}/>
                    </StyledHorizontalFeatured>
                );
            }}
        </Transition>
        }
    </TransitionGroup>;
};
type QueueComponentProps = {
    shouldShow: boolean;
}
const QueueComponent = (VARIANT: "vertical" | "horizontal") => ({ shouldShow }: QueueComponentProps) => {
    const location = c.WIDGET_POSITIONS.queue;
    const [height, setHeight] = useState<number>(VARIANT === "horizontal" ? undefined : location.sizeY - 200);
    const width = location.sizeX - c.QUEUE_WRAP_PADDING * 2;
    const [headerWidth, setHeaderWidth] = useState<number>(0);
    const [featured, queueRows] = VARIANT === "horizontal" ? useHorizontalQueueRowsData({ height, width, ftsRowWidth: width - headerWidth }):
        useVerticalQueueRowsData({ height, width });
    const RowsContainerComponent = VARIANT === "horizontal" ? HorizontalRowsContainer : RowsContainer;
    return (
        <>
            <HorizontalFeatured runInfo={featured}/>
            <QueueWrap hasFeatured={!!featured} variant={VARIANT}>
                <QueueHeader ref={(el) => (el != null) && setHeaderWidth(el.getBoundingClientRect().width)}>
                    <Title>
                        {c.QUEUE_TITLE}
                    </Title>
                    <Caption>
                        {c.QUEUE_CAPTION}
                    </Caption>
                </QueueHeader>
                <RowsContainerComponent ref={(el) => {
                    if (el != null) {
                        const bounding = el.getBoundingClientRect();
                        setHeight(bounding.height);
                    }
                }}>
                    <TransitionGroup>
                        {shouldShow && queueRows.map(row => (
                            <Transition key={row.id} timeout={c.QUEUE_ROW_APPEAR_TIME}>
                                {state => {
                                    return state !== "exited" && (
                                        <QueueRowAnimator
                                            bottom={row.bottom}
                                            right={row.right}
                                            zIndex={row.zIndex}
                                            fts={row.isFts}
                                            horizontal={true}
                                            {...queueRowContractionStates(c.QUEUE_ROW_HEIGHT)[state]}
                                        >
                                            <QueueRow runInfo={row}/>
                                        </QueueRowAnimator>
                                    );
                                }}
                            </Transition>
                        ))}
                    </TransitionGroup>
                </RowsContainerComponent>
            </QueueWrap>
        </>
    );
};

const VerticalQueue = QueueComponent("vertical");
const HorizontalQueue = QueueComponent("horizontal");

type QueueProps = {
    widgetData: Widget.QueueWidget,
};
export const Queue = ({ widgetData: { settings: { horizontal } } }: QueueProps) => {
    const shouldShow = useDelayedBoolean(300);
    return (
        <>
            {!horizontal && <VerticalQueue shouldShow={shouldShow} />}
            {horizontal && <HorizontalQueue shouldShow={shouldShow} />}
        </>
    );
};
Queue.shouldCrop = false;
Queue.zIndex=1;
export default Queue;
