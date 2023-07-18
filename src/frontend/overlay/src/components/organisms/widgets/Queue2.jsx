// import PropTypes from "prop-types";
import { useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    QUEUE_BACKGROUND_COLOR,
    QUEUE_BASIC_ZINDEX, QUEUE_CAPTION, QUEUE_MAX_ROWS,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT2,
    QUEUE_ROW_TRANSITION_TIME
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ShrinkingBox2 } from "../../atoms/Box2";
import { formatScore } from "../../atoms/ContestCells";
import { RankLabel, RunStatusLabel2 } from "../../atoms/ContestLabels2";
import { ProblemLabel } from "../../atoms/ProblemLabel";
// import { QueueRow } from "../../molecules/queue/QueueRow";
import { TeamViewHolder } from "../holder/TeamViewHolder";
import { useWithTimeoutAfterRender } from "../../../utils/hooks/withTimeoutAfterRender";

// const MAX_QUEUE_ROWS_COUNT = 20;

// Needed just for positioning and transitions. Don't use for anything else
const QueueRowAnimator = styled.div.attrs(({ bottom, zIndex }) => ({
    style: {
        bottom: bottom + "px",
        zIndex: zIndex,
    }
}))`
  overflow: hidden;
  width: 100%;

  position: absolute;
  transition: bottom linear ${({ fts }) => fts ? QUEUE_ROW_FTS_TRANSITION_TIME : QUEUE_ROW_TRANSITION_TIME}ms;
  animation: ${({ animation }) => animation} ${QUEUE_ROW_APPEAR_TIME}ms linear;
  animation-fill-mode: forwards;
  
  // styles below are needed for the padding-bottom based animation
  // If you remove any one of them - the animation breaks
  // And god knows why
  box-sizing: border-box;
  display: flex;
  height: 25px;
  align-items: flex-end;
`;

const FeaturedRunQueueRow = styled.div.attrs(({ zIndex, height }) => ({
    style: {
        zIndex: zIndex,
        height: height,
    }
}))`
  width: 100%;
  overflow: hidden;
  position: relative;
  animation: ${({ animation }) => animation} ${QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-in-out;
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

const rowContract = (fullHeight) => keyframes`
  from {
    padding-bottom: 0;
  }

  to {
    padding-bottom: ${fullHeight}px;
  }
`;

const contractionStatesFeatured = (fullHeight) => ({
    entering: {},
    entered: {
        // animation: rowExpand(fullHeight),
    },
    exiting: {
        // animation: rowContract(fullHeight),
    },
    exited: {},
});

const contractionStates = (fullHeight) => ({
    entering: {
        animation: rowExpand(fullHeight),
        style: {alignItems: "flex-start"},
    },
    entered: {},
    exiting: {
        animation: rowContract(fullHeight),
        // style: {alignItems: "flex-start"},
    },
    exited: {},
});

const useQueueRowsData = ({
    // width,
    height,
    basicZIndex = QUEUE_BASIC_ZINDEX,
}) => {
    const isNotShownYet = useWithTimeoutAfterRender(300);

    const { queue, totalQueueItems } = useSelector(state => state.queue);

    const [isFeaturedRunMediaLoaded, setIsFeaturedRunMediaLoaded] = useState(false);

    let rows = [];
    let featured = null;
    let totalFts = 0;
    queue.forEach((run, runIndex) => {
        const row = {
            ...run,
            isEven: (totalQueueItems - runIndex) % 2 === 0,
            zIndex: basicZIndex - runIndex + totalQueueItems,
            bottom: 0,
            isFeatured: false,
            isFeaturedRunMediaLoaded: false,
            isFts: run.result?.isFirstToSolveRun ?? false,
        };
        if (row.isFts) {
            totalFts++;
        }
        if (run.featuredRunMedia && featuredRunsRow.length === 0) {
            row.isFeatured = true;
            row.isFeaturedRunMediaLoaded = isFeaturedRunMediaLoaded;
            row.setIsFeaturedRunMediaLoaded = setIsFeaturedRunMediaLoaded;
            if (isFeaturedRunMediaLoaded) {
                featured = row;
            } else {
                rows.push(row);
            }
        } else {
            rows.push(row);
        }
    });
    if (isNotShownYet) {
        return [null, rows];
    }
    let ftsRowCount = 0;
    let regularRowCount = 0;
    rows.forEach((row) => {
        if (row.isFts) {
            row.bottom = (height - (QUEUE_ROW_HEIGHT2 + 3) * (totalFts - ftsRowCount)) + 3;
            console.log(row.bottom);
            console.log(height);
            ftsRowCount++;
        } else {
            row.bottom = (QUEUE_ROW_HEIGHT2 + 3) * regularRowCount;
            regularRowCount++;
        }
    });
    const allowedRegular = QUEUE_MAX_ROWS - ftsRowCount;
    rows = rows.filter((row, index) => {
        return row.isFts || index < allowedRegular;
    });
    return [featured, rows];
};

const QueueRankLabel = styled(RankLabel)`
  width: 32px;
  align-self: stretch;
  padding-left: 4px;
  flex-shrink: 0;
`;

const QueueTeamNameLabel = styled(ShrinkingBox2)`
  flex-grow: 1;
  //flex-shrink: 0;
`;
const QueueRunStatusLabel = styled(RunStatusLabel2)`
  width: 46px;
  flex-shrink: 0;
`;

const StyledQueueRow = styled.div`
  width: 100%;
  height: 25px;
  display: flex;
  align-items: center;
  border-radius: 16px;
  overflow: hidden;
  gap: 5px;
  color: white;
  font-size: 18px;
  background: rgba(0, 0, 0, 0.08);
`;

const QueueScoreLabel = styled(ShrinkingBox2)`
  width: 51px;
  flex-shrink: 0;
  flex-direction: row-reverse;
`;
const QueueProblemLabel = styled(ProblemLabel)`
  flex-shrink: 0;
`;
const QueueRightPart = styled.div`
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap;
`;
export const QueueRow = ({ runInfo, flashing }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[runInfo.teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[runInfo.teamId]);
    const probData = useSelector((state) => state.contestInfo.info?.problemsId[runInfo.problemId]);

    return <StyledQueueRow medal={scoreboardData?.medalType} flashing={flashing}>
        <QueueRankLabel rank={scoreboardData?.rank} medal={scoreboardData?.medalType}/>
        <QueueTeamNameLabel text={teamData?.shortName ?? "??"}/>
        <QueueScoreLabel align={"right"}
                         text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
        />
        <QueueRightPart>
            <QueueProblemLabel letter={probData?.letter} problemColor={probData?.color}/>
            <QueueRunStatusLabel runInfo={runInfo}/>
        </QueueRightPart>
    </StyledQueueRow>;
};


const FeaturedRunRow2 = ({ isFeatured, isLoaded, setIsLoaded, height, media, zIndex }) => {
    return <>
        <TransitionGroup component={null}>
            {isFeatured && (
                <Transition timeout={QUEUE_ROW_FEATURED_RUN_APPEAR_TIME} in={isLoaded}>
                    {state => {
                        const actualState = state === "entering" && !isLoaded ? "exited" : state;
                        const actualHeight = state !== "exited" && isLoaded ? height : 0;
                        if (state === "exited" && isLoaded) {
                            setIsLoaded(false);
                        }
                        console.log(state, actualState, Date.now() / 1000, height, actualHeight);
                        return (
                            <FeaturedRunQueueRow
                                height={state !== "exited" ? height : 0}
                                zIndex={zIndex}
                                {...contractionStatesFeatured(height)[state]}
                            >
                                <TeamViewHolder media={media} onLoadStatus={setIsLoaded} borderRadius="16px"/>
                            </FeaturedRunQueueRow>
                        );
                    }}
                </Transition>
            )}
        </TransitionGroup>
    </>;
};

const QueueWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${QUEUE_BACKGROUND_COLOR};
  background-repeat: no-repeat;
  border-radius: 16px;
  padding: 8px;
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
  font-size: 32px;
  font-weight: 700;
  line-height: 44px;
  color: white;
  width: 100%;
  display: flex;
`;

const Title = styled.div`
  flex: 1 0 0;
`;

const Caption = styled.div`
`;

export const Queue2 = ({ }) => {
    const [width, setWidth] = useState(null);
    const [height, setHeight] = useState(null);
    const [featured, queueRows] = useQueueRowsData({ width, height });

    return <QueueWrap>
        <QueueHeader>
            <Title>
                Queue
            </Title>
            <Caption>
                {QUEUE_CAPTION}
            </Caption>
        </QueueHeader>
        <RowsContainer ref={(el) => {
            if (el != null) {
                const bounding = el.getBoundingClientRect();
                setWidth(bounding.width);
                setHeight(bounding.height);
            }
        }}>
            <TransitionGroup component={null}>
                {queueRows.map(row => (
                    <Transition key={row.id} timeout={QUEUE_ROW_APPEAR_TIME}>
                        {state => {
                            return state !== "exited" && (
                                <QueueRowAnimator
                                    bottom={row.bottom}
                                    zIndex={row.zIndex}
                                    fts={row.isFts}
                                    {...contractionStates(QUEUE_ROW_HEIGHT2)[state]}
                                >
                                    {/*<FeaturedRunRow2*/}
                                    {/*    isFeatured={row.isFeatured}*/}
                                    {/*    media={row.featuredRunMedia}*/}
                                    {/*    isLoaded={row.isFeaturedShown}*/}
                                    {/*    setIsLoaded={row.setIsFeaturedRunMediaLoaded}*/}
                                    {/*    height={row.featuredRunMediaHeight}*/}
                                    {/*    zIndex={QUEUE_BASIC_ZINDEX + 20}*/}
                                    {/*/>*/}
                                    <QueueRow runInfo={row} isEven={row.isEven}
                                              flashing={row.isFeatured && !row.isFeaturedRunMediaLoaded}/>
                                </QueueRowAnimator>
                            );
                        }}
                    </Transition>
                ))}
            </TransitionGroup>
        </RowsContainer>
    </QueueWrap>;
};
Queue2.shouldCrop = false;
export default Queue2;
