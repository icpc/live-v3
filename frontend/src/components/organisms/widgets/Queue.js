import _ from "lodash";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    QUEUE_FTS_PADDING,
    QUEUE_ROW_APPEAR_TIME,
    QUEUE_ROW_FTS_TRANSITION_TIME,
    QUEUE_ROW_HEIGHT,
    QUEUE_ROW_TRANSITION_TIME,
    QUEUE_ROWS_COUNT
} from "../../../config";
import { QueueRow } from "../../molecules/queue/QueueRow";

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  //background-color: gray;
  position: relative;
`;

const BreakingNewsContainer = styled.div`

`;

const QueueContainer = styled.div`

`;

const QueueWrap = styled.div`
  height: ${QUEUE_ROW_HEIGHT * QUEUE_ROWS_COUNT}px;
  width: 100%;
  bottom: 0;
  position: absolute;

  background-color: lightblue;
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
  box-sizing: border-box;
`;

const rowExpand = keyframes`
  from {
    max-height: 0;
  }

  to {
    max-height: ${QUEUE_ROW_HEIGHT}px;
  }
`;

const rowContract = keyframes`
  from {
    max-height: ${QUEUE_ROW_HEIGHT}px;
  }

  to {
    max-height: 0;
  }
`;


const transitionProps = {
    entering: { animation: rowExpand },
    entered: {},
    exiting: { animation: rowContract },
    exited: {},
};

export const Queue = () => {
    const queue = useSelector(state => state.queue.queue);
    const [justRendered, setJustRendered] = useState(true);
    let allRows = [];
    let queueRowsCount = 0;
    for (let queueEntry of _.sortBy(queue, "isFirstSolvedRun")) {
        let bottom = QUEUE_ROW_HEIGHT * queueRowsCount;
        if (queueEntry.isFirstSolvedRun) {
            bottom = queueRowsCount * QUEUE_ROW_HEIGHT + QUEUE_FTS_PADDING * (queueRowsCount > 0);
        }
        if (justRendered) {
            bottom = 0;
        }
        const el =
            <Transition key={queueEntry.id} timeout={QUEUE_ROW_APPEAR_TIME}>
                {state =>
                    state !== "exited" &&
                    <QueueRowWrap bottom={bottom} fts={queueEntry.isFirstSolvedRun}
                        {...transitionProps[state]}>
                        <QueueRow entryData={queueEntry}/>
                    </QueueRowWrap>
                }
            </Transition>;
        allRows.unshift(el);
        queueRowsCount += 1;
    }
    useEffect(() => {
        setJustRendered(false);
    }, [justRendered]);
    return <WidgetWrap>
        <TransitionGroup component={null}>
            {allRows}
        </TransitionGroup>
    </WidgetWrap>;
};
export default Queue;
