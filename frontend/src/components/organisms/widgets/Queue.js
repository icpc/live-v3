import _ from "lodash";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { QUEUE_FTS_PADDING, QUEUE_ROW_HEIGHT, QUEUE_ROW_TRANSITION_TIME, QUEUE_ROWS_COUNT } from "../../../config";
import { QueueRow } from "../../molecules/queue/QueueRow";

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  
  position: relative;
`;

const BreakingNewsContainer = styled.div`
    
`;

const QueueContainer = styled.div`
    
`;

const QueueWrap = styled.div`
  height: ${QUEUE_ROW_HEIGHT*QUEUE_ROWS_COUNT}px;
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
    position: absolute;
    transition: bottom linear ${QUEUE_ROW_TRANSITION_TIME}ms;
    width: 100%;
`;

export const Queue = () => {
    const queue = useSelector(state => state.queue.queue);
    const [justRendered, setJustRendered] = useState(true);
    const allRows = [];
    let queueRowsCount = 0;
    let firstToSolveCount = 0;
    for(let queueEntry of _.sortBy(queue, "isFirstSolvedRun")) {
        let bottom = QUEUE_ROW_HEIGHT * queueRowsCount;
        if (queueEntry.isFirstSolvedRun) {
            bottom = queueRowsCount * QUEUE_ROW_HEIGHT + QUEUE_FTS_PADDING * (queueRowsCount > 0);
        }
        if(justRendered) {
            bottom = 0;
        }
        const el = <QueueRowWrap key={queueEntry.id} bottom={bottom}>
            <QueueRow entryData={queueEntry}/>
        </QueueRowWrap>;
        allRows.push(el);
        queueRowsCount += !queueEntry.isFirstSolvedRun;
        firstToSolveCount += !!queueEntry.isFirstSolvedRun;
    }
    useEffect(() => {
        setJustRendered(false);
    }, [justRendered]);
    return <WidgetWrap>
        {allRows}
    </WidgetWrap>;
};
export default Queue;
