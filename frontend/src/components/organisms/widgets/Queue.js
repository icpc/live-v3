import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { QUEUE_ROW_HEIGHT, QUEUE_ROW_TRANSITION_TIME, QUEUE_ROWS_COUNT } from "../../../config";
import { QueueRow } from "../../molecules/queue/QueueRow";

const WidgetWrap = styled.div`
  background-color: grey;
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
    for(let queueEntry of queue) {
        let bottom = QUEUE_ROW_HEIGHT * queueRowsCount;
        if (queueEntry.isFirstSolvedRun) {
            bottom = (QUEUE_ROWS_COUNT - firstToSolveCount) * QUEUE_ROW_HEIGHT + QUEUE_ROW_HEIGHT/2;
        }
        if(justRendered) {
            bottom = 0;
        }
        const el = <QueueRowWrap key={queueEntry.id} bottom={bottom}>
            <QueueRow entryData={queueEntry} teamData={undefined}/>
        </QueueRowWrap>;
        allRows.push(el);
        queueRowsCount += !queueEntry.isFirstSolvedRun;
        firstToSolveCount += !!queueEntry.isFirstSolvedRun;
    }
    useEffect(() => {
        setJustRendered(false);
    }, [justRendered]);
    return <WidgetWrap>
        {/*{JSON.stringify(queue)}*/}
        {allRows}
    </WidgetWrap>;
};
export default Queue;
