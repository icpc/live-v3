import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    QUEUE_BASIC_ZINDEX,
    QUEUE_FEATURED_RUN_ASPECT,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT2,
    QUEUE_ROW_TRANSITION_TIME, WIDGET_LAYOUT_BACKGROUND
} from "../../../config";
import { QueueRow2 } from "../../molecules/queue/QueueRow2";
import { TeamViewHolder } from "../holder/TeamViewHolder";
import { useWithTimeoutAfterRender } from "../../../utils/hooks/withTimeoutAfterRender";

const MAX_QUEUE_ROWS_COUNT = 20;

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${WIDGET_LAYOUT_BACKGROUND};
  background-repeat: no-repeat;
  border-radius: 16px;
`;

const QueueRowWrap = styled.div.attrs(({ bottom, zIndex }) => ({
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

const contractionStatesFeature = (fullHeight) => ({
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
            bottom: -QUEUE_ROW_HEIGHT2,
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
        row.bottom = QUEUE_ROW_HEIGHT2 * rowIndex;
    });
    return [...featuredRunsRow, ...ftsRunsRows, ...regularRunsRows];
};

const FeaturedRunRow2 = ({ isFeatured, isLoaded, setIsLoaded, height, media, zIndex }) => {
    const [isReady, setIsReady] = useState(false);
    useEffect(() => {
        if (isLoaded) {
            console.log("media ready", Date.now() / 1000);
        }
        setIsReady(true);
    }, [isLoaded]);
    return (
        <TransitionGroup>
            {isFeatured && (
                <Transition timeout={QUEUE_ROW_FEATURED_RUN_APPEAR_TIME} in={isReady}>
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
                                {...contractionStatesFeature(height)[state]}
                            >
                                <TeamViewHolder media={media} onLoadStatus={(v) => {
                                    console.log("media loaded in sec", Date.now() / 1000);
                                    if (v) {
                                        setTimeout(() => setIsLoaded(v), 1000);
                                    } else {
                                        setIsLoaded(false);
                                    }
                                }} borderRadius="16px"/>
                            </FeaturedRunQueueRow>
                        );
                    }}
                </Transition>
            )}
        </TransitionGroup>
    );
};


export const Queue2 = ({ widgetData }) => {
    const { sizeX: width, sizeY: height } = widgetData.location;
    const queueRows = useQueueRowsData({ width, height });

    return <WidgetWrap>
        <TransitionGroup component={null}>
            {queueRows.map(row => (
                <Transition key={row.id} timeout={QUEUE_ROW_APPEAR_TIME}>
                    {state => {
                        return state !== "exited" && (
                            <QueueRowWrap
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
                                <QueueRow2 runInfo={row} isEven={row.isEven} flashing={row.isFeatured && !row.isFeaturedShown}/>
                            </QueueRowWrap>
                        );
                    }}
                </Transition>
            ))}
        </TransitionGroup>
    </WidgetWrap>;
};

export default Queue2;
