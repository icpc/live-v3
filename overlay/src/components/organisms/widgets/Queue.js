import _ from "lodash";
import React, { useCallback, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    QUEUE_FEATURED_RUN_ASPECT,
    QUEUE_FTS_PADDING,
    QUEUE_OPACITY,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY,
    QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT,
    QUEUE_ROW_TRANSITION_TIME
} from "../../../config";
import { QueueRow } from "../../molecules/queue/QueueRow";
import { TeamViewHolder } from "./TeamView";

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  opacity: ${QUEUE_OPACITY};
  position: relative;
`;

const QueueRowWrap = styled.div.attrs(({ bottom, zindex }) => ({
    style: {
        bottom: bottom + "px", zIndex: zindex
    }
}))`
  overflow: hidden;
  width: 100%;

  position: absolute;
  display: flex;
  flex-direction: column;
  transition: bottom linear ${props => props.fts ? QUEUE_ROW_FTS_TRANSITION_TIME : QUEUE_ROW_TRANSITION_TIME}ms;
  animation: ${props => props.animation} ${QUEUE_ROW_APPEAR_TIME}ms linear;
  animation-fill-mode: forwards;
  box-sizing: border-box;
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
  animation: ${props => props.animation} ${QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-in-out;
  animation-fill-mode: forwards;
`;

const FeaturedRunRow = ({ featured, featuredRunHeight, onLoad }) => {
    // FIXME
    // my brain can't think atm
    // I have no idea how to implement featured run better at this time.
    // Ideally this should all be managed from the allrows.
    // As if the featured run row is a row on itself.
    // But then it would be harder to animate it
    const [loaded, setLoaded] = useState(false);
    const [lastMedia, setLastMedia] = useState(featured); // FIXME: hack
    useEffect(() => {
        if (featured !== undefined) {
            setLastMedia(featured);
        } else {
            setTimeout(() => {
                setLoaded(false);
                setLastMedia(undefined);
            }, QUEUE_ROW_FEATURED_RUN_APPEAR_TIME + 100); // FIXME: hack
        }
    }, [featured]);
    const onLoadStatus = useCallback((v) => {
        setTimeout(() => {
            setLoaded(v);
            onLoad();
        }, QUEUE_ROW_FEATURED_RUN_ADDITIONAL_DELAY);
    }, [onLoad]);
    return <Transition timeout={QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}
        in={featured && loaded}
    >
        {state =>
            <QueueTeamViewContainer
                height={state !== "exited" ? featuredRunHeight : 0}
                {...contractionStates(featuredRunHeight)[state]}
            >
                {lastMedia && <TeamViewHolder media={lastMedia}
                    onLoadStatus={onLoadStatus}/>
                }
            </QueueTeamViewContainer>}
    </Transition>;
};


export const Queue = ({ widgetData }) => {
    const { sizeX: width, sizeY: height } = widgetData.location;
    const { queue, totalQueueItems } = useSelector(state => state.queue);
    const [isJustShown, setIsJustShown] = useState(true);
    let allRows = [];
    let queueRowsCount = 0;
    const featuredRunHeight = width / QUEUE_FEATURED_RUN_ASPECT;
    const [featuredRunLoaded, setFeaturedRunLoaded] = useState(false);
    const hasFeatured = queue.some((e) => e.featuredRunMedia !== undefined);
    useEffect(() => {
        if (!hasFeatured) {
            setFeaturedRunLoaded(false);
        }
    }, [hasFeatured]);
    for (let queueEntry of _.sortBy(queue, ["isFirstSolvedRun"])) {
        let bottom = QUEUE_ROW_HEIGHT * queueRowsCount;
        if (queueEntry.isFirstSolvedRun) {
            bottom += QUEUE_FTS_PADDING * (queueRowsCount > 0);
        }
        const featured = queueEntry.featuredRunMedia;
        if (featured !== undefined && featuredRunLoaded) {
            bottom = height - featuredRunHeight - QUEUE_ROW_HEIGHT;
            queueRowsCount -= 1;
        }
        if (isJustShown) {
            bottom = -QUEUE_ROW_HEIGHT;
        }
        const isEven = (totalQueueItems - queueRowsCount) % 2 === 0;
        const el = <Transition key={queueEntry.id} timeout={QUEUE_ROW_APPEAR_TIME}>
            {state => {
                return state !== "exited" && <QueueRowWrap bottom={bottom}
                    zindex={featured !== undefined ? 2147000000 : queueEntry.time}
                    {...contractionStates(QUEUE_ROW_HEIGHT)[state]}>
                    <FeaturedRunRow
                        onLoad={() => setFeaturedRunLoaded(true)}
                        featured={featured}
                        featuredRunHeight={featuredRunHeight}
                    />
                    <QueueRow entryData={queueEntry} isEven={isEven} flash={featured && !featuredRunLoaded}/>
                </QueueRowWrap>;
            }}
        </Transition>;
        allRows.push(el);
        queueRowsCount += 1;
    }
    setTimeout(() => setIsJustShown(false), 300); // FIXME: this is a hack
    return <WidgetWrap>
        <TransitionGroup component={null}>
            {allRows}
        </TransitionGroup>
    </WidgetWrap>;
};
export default Queue;
