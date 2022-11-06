import _ from "lodash";
import React, { useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    QUEUE_FEATURED_RUN_ASPECT,
    QUEUE_FTS_PADDING,
    QUEUE_OPACITY,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT,
    QUEUE_ROW_TRANSITION_TIME
} from "../../../config";
import { QueueRow } from "../../molecules/queue/QueueRow";
import { TeamViewHolder } from "../holder/TeamVeiwHolder";

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  opacity: ${QUEUE_OPACITY};
  position: relative;
`;

const QueueRowWrap = styled.div.attrs(({ bottom }) => ({
    style: {
        bottom: bottom + "px"
    }
}))`
  overflow: hidden;
  width: 100%;

  position: absolute;
  transition: bottom linear ${props => props.fts ? QUEUE_ROW_FTS_TRANSITION_TIME : QUEUE_ROW_TRANSITION_TIME}ms;
  animation: ${props => props.animation} ${QUEUE_ROW_APPEAR_TIME}ms linear;
  animation-fill-mode: forwards;
  box-sizing: border-box;
  z-index: ${props => props.zindex};
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


const contractionStates = (fullHeight) => ({
    entering: { animation: rowExpand(fullHeight) },
    entered: {},
    exiting: { animation: rowContract(fullHeight) },
    exited: {},
});

const QueueTeamViewContainer = styled.div`
  width: 100%;
  height: ${props => props.height}px;
  overflow: hidden;
  position: relative;
  animation: ${props => props.animation} ${QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms linear;
  animation-fill-mode: forwards;
`;


export const Queue = ({ widgetData }) => {
    const { sizeX: width, sizeY: height } = widgetData.location;
    const { queue, totalQueueItems } = useSelector(state => state.queue);
    const [isJustShown, setIsJustShown] = useState(true);
    let allRows = [];
    let queueRowsCount = 0;
    const featuredRunHeight = width / QUEUE_FEATURED_RUN_ASPECT;
    const [featuredRunLoaded, setFeaturedRunLoaded] = useState(false);
    for (let queueEntry of _.sortBy(queue, ["isFirstSolvedRun"])) {
        let bottom = QUEUE_ROW_HEIGHT * queueRowsCount;
        if (queueEntry.isFirstSolvedRun) {
            bottom += QUEUE_FTS_PADDING * (queueRowsCount > 0);
        }
        const featuredRunMedia = queueEntry.featuredRunMedia;
        if (featuredRunMedia !== undefined && featuredRunLoaded === true) {
            bottom = height - featuredRunHeight - QUEUE_ROW_HEIGHT;
            queueRowsCount -= 1;
        }
        if (isJustShown) {
            bottom = -QUEUE_ROW_HEIGHT;
        }
        const isEven = (totalQueueItems - queueRowsCount) % 2 === 0;
        const el =
            <Transition key={queueEntry.id} timeout={QUEUE_ROW_APPEAR_TIME}>
                {state =>
                    state !== "exited" &&
                    <QueueRowWrap bottom={bottom}
                        zindex={featuredRunMedia !== undefined ? 2147000000 : queueEntry.time}
                        {...contractionStates(QUEUE_ROW_HEIGHT)[state]}>
                        <Transition timeout={QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}
                            in={featuredRunMedia && featuredRunLoaded}>
                            {state => {
                                console.log("state", state);
                                return featuredRunMedia && <QueueTeamViewContainer
                                    height={featuredRunHeight}
                                    {...contractionStates(featuredRunHeight)[state]}
                                >
                                    <TeamViewHolder media={featuredRunMedia}
                                        onLoadStatus={() => setFeaturedRunLoaded(true)}/>
                                </QueueTeamViewContainer>;
                            }
                            }
                        </Transition>
                        <QueueRow entryData={queueEntry} isEven={isEven}/>
                    </QueueRowWrap>
                }
            </Transition>;
        allRows.push(el);
        queueRowsCount += 1;
    }
    setTimeout(() => setIsJustShown(false), 1000); // FIXME: this is a hack
    return <WidgetWrap>
        <TransitionGroup component={null}>
            {allRows}
        </TransitionGroup>
    </WidgetWrap>;
};
export default Queue;
