import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    CELL_QUEUE_RANK_WIDTH2,
    CELL_QUEUE_TOTAL_SCORE_WIDTH,
    CELL_QUEUE_VERDICT_WIDTH2,
    CONTESTER_ROW_VERDICT_FONT_SIZE2,
    QUEUE_BACKGROUND_COLOR,
    QUEUE_BASIC_ZINDEX, QUEUE_CAPTION,
    QUEUE_FEATURED_RUN_ASPECT,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT2,
    QUEUE_ROW_TRANSITION_TIME,
    QUEUE_VERDICT_PADDING_LEFT2,
    WIDGET_LAYOUT_BACKGROUND
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { FlexedBox2, ShrinkingBox2 } from "../../atoms/Box2";
import { ContestantRow2 } from "../../atoms/ContestantRow2";
import { formatScore } from "../../atoms/ContestCells";
import { ProblemCircleLabel, RankLabel, RunStatusLabel2 } from "../../atoms/ContestLabels2";
// import { QueueRow } from "../../molecules/queue/QueueRow";
import { TeamViewHolder } from "../holder/TeamViewHolder";
import { useWithTimeoutAfterRender } from "../../../utils/hooks/withTimeoutAfterRender";

const MAX_QUEUE_ROWS_COUNT = 20;

const QueueRowContainer = styled.div.attrs(({ bottom, zIndex }) => ({
    style: {
        bottom: bottom + "px",
        zIndex: zIndex,
    }
}))`
  overflow: hidden;
  width: 100%;

  position: absolute;
  display: flex;
  flex-direction: column;
  transition: bottom linear ${({ fts }) => fts ? QUEUE_ROW_FTS_TRANSITION_TIME : QUEUE_ROW_TRANSITION_TIME}ms;
  animation: ${({ animation }) => animation} ${QUEUE_ROW_APPEAR_TIME}ms linear;
  animation-fill-mode: forwards;
  box-sizing: border-box;
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
    max-height: ${fullHeight}px;
  }

  to {
    max-height: 0;
  }
`;

const contractionStatesFeatured = (fullHeight) => ({
    entering: {},
    entered: { animation: rowExpand(fullHeight) },
    exiting: { animation: rowContract(fullHeight) },
    exited: {},
});

const contractionStates = (fullHeight) => ({
    entering: { animation: rowExpand(fullHeight) },
    entered: {},
    exiting: { animation: rowContract(fullHeight) },
    exited: {},
});

const useQueueRowsData = ({
    width,
    height,
    basicZIndex = QUEUE_BASIC_ZINDEX,
}) => {
    const isNotShownYet = useWithTimeoutAfterRender(300);
    const featuredRunHeight = width / QUEUE_FEATURED_RUN_ASPECT;

    const { queue, totalQueueItems } = useSelector(state => state.queue);

    const [isFeaturedRunMediaLoaded, setIsFeaturedRunMediaLoaded] = useState(false);

    const featuredRunsRow = [];
    const ftsRunsRows = [];
    const regularRunsRows = [];
    queue.forEach((run, runIndex) => {
        const row = {
            ...run,
            isEven: (totalQueueItems - runIndex) % 2 === 0,
            zIndex: basicZIndex + runIndex,
            bottom: 0,
            isFeatured: false,
            isFeaturedShown: false,
            isFts: run.result?.isFirstToSolveRun ?? false,
        };
        if (run.featuredRunMedia && featuredRunsRow.length === 0) {
            row.isFeatured = true;
            row.isFeaturedShown = isFeaturedRunMediaLoaded;
            row.setIsFeaturedRunMediaLoaded = setIsFeaturedRunMediaLoaded;
            row.featuredRunMediaHeight = featuredRunHeight;
            if (isFeaturedRunMediaLoaded) {
                featuredRunsRow.push(row);
            } else {
                if (run.result?.isFirstToSolveRun) {
                    ftsRunsRows.push(row);
                } else {
                    regularRunsRows.push(row);
                }
            }
        } else if (run.result?.isFirstToSolveRun) {
            ftsRunsRows.push(row);
        } else {
            regularRunsRows.push(row);
        }
    });
    if (isNotShownYet) {
        return [...featuredRunsRow, ...ftsRunsRows, ...regularRunsRows];
    }
    featuredRunsRow.forEach((row) => {
        row.zIndex = basicZIndex + MAX_QUEUE_ROWS_COUNT;
        row.isEven = false;
        row.bottom = height - featuredRunHeight - QUEUE_ROW_HEIGHT2;
    });
    ftsRunsRows.forEach((row, rowIndex) => {
        row.bottom = QUEUE_ROW_HEIGHT2 * (rowIndex + regularRunsRows.length + 1 * (regularRunsRows.length > 0));
    });
    regularRunsRows.forEach((row, rowIndex) => {
        row.bottom = (QUEUE_ROW_HEIGHT2 + 3) * rowIndex;
    });
    return [...featuredRunsRow, ...ftsRunsRows, ...regularRunsRows];
};

const QueueRankLabel = styled(RankLabel)`
  width: ${CELL_QUEUE_RANK_WIDTH2};
  margin-right: 6px;
`;

const QueueTeamNameLabel = styled(ShrinkingBox2)`
  flex-grow: 1;
  flex-shrink: 1;
`;
const QueueRunStatusLabel = styled(RunStatusLabel2)`
  width: ${CELL_QUEUE_RANK_WIDTH2};
  margin-left: ${QUEUE_VERDICT_PADDING_LEFT2};
  margin-top: 6px;
  font-size: ${CONTESTER_ROW_VERDICT_FONT_SIZE2};
`;
const QueueRowWrap = styled.div`
  display: flex;
  background: rgba(0, 0, 0, 0.08);
`;
export const QueueRow = ({ runInfo, isEven, flashing }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[runInfo.teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[runInfo.teamId]);
    const probData = useSelector((state) => state.contestInfo.info?.problemsId[runInfo.problemId]);

    return <QueueRowWrap medal={scoreboardData?.medalType} flashing={flashing}>
        <QueueRankLabel rank={scoreboardData?.rank}/>
        <QueueTeamNameLabel text={teamData?.shortName ?? "??"}/>
        <ShrinkingBox2 width={CELL_QUEUE_TOTAL_SCORE_WIDTH} align={"center"}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
        />
        <ProblemCircleLabel letter={probData?.letter} problemColor={probData?.color} />
        <QueueRunStatusLabel runInfo={runInfo}/>
    </QueueRowWrap>;
};

QueueRow.propTypes = {
    runInfo: PropTypes.object.isRequired,
    isEven: PropTypes.bool.isRequired
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
                                height={state !== "exited"? height : 0}
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
  color: white;
  width: 100%;
  display: flex;
`;

const Title = styled.div`
  flex-grow: 1
`;

const Caption = styled.div`
`;

export const Queue2 = ({ widgetData }) => {
    const { sizeX: width, sizeY: height } = widgetData.location;
    const queueRows = useQueueRowsData({ width, height });

    return <QueueWrap>
        <QueueHeader>
            <Title>
                Queue
            </Title>
            <Caption>
                {QUEUE_CAPTION}
            </Caption>
        </QueueHeader>
        <RowsContainer>
            <TransitionGroup component={null}>
                {queueRows.map(row => (
                    <Transition key={row.id} timeout={QUEUE_ROW_APPEAR_TIME}>
                        {state => {
                            return state !== "exited" && (
                                <QueueRowContainer
                                    bottom={row.bottom}
                                    zIndex={row.zIndex}
                                    fts={row.isFts}
                                    {...contractionStates(QUEUE_ROW_HEIGHT2)[state]}
                                >
                                    <FeaturedRunRow2
                                        isFeatured={row.isFeatured}
                                        media={row.featuredRunMedia}
                                        isLoaded={row.isFeaturedShown}
                                        setIsLoaded={row.setIsFeaturedRunMediaLoaded}
                                        height={row.featuredRunMediaHeight}
                                        zIndex={QUEUE_BASIC_ZINDEX + 20}
                                    />
                                    <QueueRow runInfo={row} isEven={row.isEven} flashing={row.isFeatured && !row.isFeaturedShown}/>
                                </QueueRowContainer>
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
