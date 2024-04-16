// import PropTypes from "prop-types";
import { useCallback, useMemo, useState } from "react";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { css, CSSObject, Keyframes, keyframes } from "styled-components";
import c from "../../../config";
import { SCOREBOARD_TYPES } from "@/consts";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { RankLabel, RunStatusLabel } from "../../atoms/ContestLabels";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { ContestantViewHolder } from "../holder/ContestantViewHolder";
import { useDelayedBoolean } from "@/utils/hooks/withTimeoutAfterRender";
import star from "../../../assets/icons/star.svg";
import star_mask from "../../../assets/icons/star_mask.svg";
import { formatScore } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";
import { Award, RunInfo, Widget } from "@shared/api";
import { isFTS } from "@/utils/statusInfo";

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
  transition-property: ${({ horizontal }) => horizontal ? "right" : "bottom"};
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

interface QueueRow extends RunInfo {
    isEven: boolean,
    zIndex: number,
    bottom: number,
    right: number,
    isFeatured: boolean,
    isFeaturedRunMediaLoaded: boolean,
    isFts: boolean,
    setIsFeaturedRunMediaLoaded: (state: boolean) => void | null,
}

const useQueueRowsData = ({
    width,
    height,
    basicZIndex = c.QUEUE_BASIC_ZINDEX,
    horizontal = true
}: {
    width: number,
    height: number,
    basicZIndex?: number,
    horizontal?: boolean,
}): [QueueRow | null, QueueRow[]] => {
    const shouldShow = useDelayedBoolean(300);

    const bottomPosition = useCallback((index: number) => {
        const actual = horizontal ? Math.floor(index / c.QUEUE_HORISONTAL_HEIGHT_NUM) : index;
        return (c.QUEUE_ROW_HEIGHT + c.QUEUE_ROW_PADDING) * actual;
    }, [horizontal]);
    const rightPosition = useCallback((index: number) => {
        return (c.QUEUE_ROW_WIDTH + c.QUEUE_ROW_HORIZONTAL_PADDING) * (horizontal ? (index % c.QUEUE_HORISONTAL_HEIGHT_NUM) : 0);
    }, [horizontal]);
    const allowedMaxRows = useMemo(() => {
        return Math.min((width / c.QUEUE_ROW_WIDTH) * (height / c.QUEUE_ROW_HEIGHT), c.QUEUE_MAX_ROWS);
    }, [width, height]);

    const { queue, totalQueueItems } = useAppSelector(state => state.queue);

    const [loadedMediaRun, setLoadedMediaRun] = useState(null);

    let rows: QueueRow[] = [];
    let featured: QueueRow | null = null;
    let totalFts = 0;
    queue.forEach((run, runIndex) => {
        const row: QueueRow = {
            ...run,
            isEven: (totalQueueItems - runIndex) % 2 === 0,
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
    if (!shouldShow) {
        return [null, rows];
    }
    let ftsRowCount = 0;
    let regularRowCount = 0;
    rows.forEach((row) => {
        if (row.isFts) {
            if (horizontal) {
                row.bottom = height - c.QUEUE_ROW_HEIGHT - 3 - bottomPosition(totalFts - ftsRowCount - 1);
                row.right = width - c.QUEUE_ROW_WIDTH - rightPosition(totalFts - ftsRowCount - 1);
            } else {
                row.bottom = height - bottomPosition(totalFts - ftsRowCount) + 3;
                row.right = 0;
            }
            ftsRowCount++;
        } else  {
            row.bottom = bottomPosition(regularRowCount);
            row.right = rightPosition(regularRowCount);
            regularRowCount++;
        }
    });
    const allowedRegular = allowedMaxRows - ftsRowCount;
    rows = rows.filter((row, index) => {
        return row.isFts || index < allowedRegular;
    });
    return [featured, rows];
};

const QueueRankLabel = styled(RankLabel)`
  width: 32px;
  align-self: stretch;
  padding-left: 12px;
  flex-shrink: 0;
`;

const QueueTeamNameLabel = styled(ShrinkingBox)`
  flex-grow: 1;
  /* flex-shrink: 0; */
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
  width: 28px;
  font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
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
// const QueueFTSStartProblemWrap = styled.div`
//   width: 28px;
//   height: 100%;
//   position: relative;
//   background: ${props => "black"};
//
//   display: flex;
//   justify-content: center;
//   align-items: center;
//
//   flex-shrink: 0;
//   mask: url(${star}) 50% 50% no-repeat;
//   mask-origin: content-box;
//   mask-clip: border-box;
//   mask-size: 25px;
//   padding: 2px;
// `;

export const QueueRow = ({ runInfo,
    // flashing
}) => {
    const scoreboardData = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[runInfo.teamId]);
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[runInfo.teamId]);
    const probData = useAppSelector((state) => state.contestInfo.info?.problemsId[runInfo.problemId]);
    const awards = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].idAwards[runInfo.teamId]);
    const rank = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].rankById[runInfo.teamId]);
    const medal = awards?.find((award) => award.type == Award.Type.medal) as Award.medal;
    const isFTSRun = runInfo?.result?.type === "ICPC" && runInfo.result.isFirstToSolveRun || runInfo?.result?.type === "IOI" && runInfo.result.isFirstBestRun;
    return <StyledQueueRow
        // flashing={flashing}
    >
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

const QueueWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${c.QUEUE_BACKGROUND_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  padding: ${c.QUEUE_ROW_HORIZONTAL_PADDING}px;
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

const QueueHeader = styled.div`
  font-size: ${c.QUEUE_HEADER_FONT_SIZE};
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  line-height: ${c.QUEUE_HEADER_LINE_HEIGHT};
  color: white;
  width: 100%;
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

const QueueTeamView = styled(ContestantViewHolder)`
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  position: relative; /* fixme: ContestantViewHolder should be semantically relative and follow the flow. */
`;

export const Featured = ({ runInfo }) => {
    return <TransitionGroup component={null}>
        {runInfo && <Transition timeout={c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME} key={runInfo.id}>
            {state => {
                const realState = runInfo.isFeaturedRunMediaLoaded ? state : "exited";
                return (
                    <StyledFeatured additional={appearStatesFeatured[realState]}>
                        <QueueTeamView media={runInfo.featuredRunMedia}
                            onLoadStatus={runInfo.setIsFeaturedRunMediaLoaded}/>
                        <QueueRow runInfo={runInfo}/>
                    </StyledFeatured>
                );
            }}
        </Transition>
        }
    </TransitionGroup>;
};

type QueueProps = {
    widgetData: Widget.QueueWidget,
};
export const Queue = ({ widgetData: { settings: { horizontal }, location } }: QueueProps) => {
    const [height, setHeight] = useState<number>(null);
    const width =location.sizeX - c.QUEUE_ROW_HORIZONTAL_PADDING * 2;
    const [featured, queueRows] = useQueueRowsData({ height, width, horizontal });
    return <>
        <Featured runInfo={featured}/>
        <QueueWrap>
            <QueueHeader>
                <Title>
                    {c.QUEUE_TITLE}
                </Title>
                <Caption>
                    {c.QUEUE_CAPTION}
                </Caption>
            </QueueHeader>
            <RowsContainer ref={(el) => {
                if (el != null) {
                    const bounding = el.getBoundingClientRect();
                    // setWidth(bounding.width);
                    setHeight(bounding.height);
                }
            }}>
                <TransitionGroup component={null}>
                    {queueRows.map(row => (
                        <Transition key={row.id} timeout={c.QUEUE_ROW_APPEAR_TIME}>
                            {state => {
                                return state !== "exited" && (
                                    <QueueRowAnimator
                                        bottom={row.bottom}
                                        right={row.right}
                                        zIndex={row.zIndex}
                                        fts={row.isFts}
                                        horizontal={horizontal}
                                        {...queueRowContractionStates(c.QUEUE_ROW_HEIGHT)[state]}
                                    >
                                        {/*<FeaturedRunRow2*/}
                                        {/*    isFeatured={row.isFeatured}*/}
                                        {/*    media={row.featuredRunMedia}*/}
                                        {/*    isLoaded={row.isFeaturedShown}*/}
                                        {/*    setIsLoaded={row.setIsFeaturedRunMediaLoaded}*/}
                                        {/*    height={row.featuredRunMediaHeight}*/}
                                        {/*    zIndex={QUEUE_BASIC_ZINDEX + 20}*/}
                                        {/*/>*/}
                                        <QueueRow runInfo={row}
                                            // isEven={row.isEven} // not used atm.
                                            // flashing={row.isFeatured && !row.isFeaturedRunMediaLoaded} // not used atm.
                                        />
                                    </QueueRowAnimator>
                                );
                            }}
                        </Transition>
                    ))}
                </TransitionGroup>
            </RowsContainer>
        </QueueWrap>
    </>;
};
Queue.shouldCrop = false;
Queue.zIndex=1;
export default Queue;
