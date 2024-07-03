import React from "react";
import styled from "styled-components";
import { useAppSelector } from "../../../redux/hooks";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ProblemResult } from "../../../../../generated/api";
import c from "../../../config";

const TimeLineContainer = styled.div`
    grid-row-start: 1;
    grid-row-end: 3;
    justify-self: end;
    align-self: end;
    grid-column-end: 3;
    align-items: center;
    width: 100%;
    position: absolute;
    z-index: 1;
    top: 10px;
    display: grid;
    grid-auto-rows: ${c.QUEUE_ROW_HEIGHT}px;
    white-space: nowrap;
`;


const Line = styled.div`
    width: 100%;
    height: 4%;
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
    position: relative;
`;

const Circle = styled.div`
    width: 20px;
    height: 20px;
    background-color: ${props => (props.solved ? c.VERDICT_OK : props.pending ? c.VERDICT_UNKNOWN : c.VERDICT_NOK)};
    border-radius: 50%;
    position: absolute;
    //left: -10px;
    top: -10px;
    align-self: center;
`;

const Label = styled.div`
    position: absolute;
    align-self: center;
    top: -10px;
    display: inline-block;
`;

const ProblemWrap = styled.div`
    display: inline-flex;
    flex-direction: column;
    align-items: center;

    position: absolute;
    top: 5%;
    left: ${props => props.leftMargin};
`;

const Problem = ({ problemResult, index, contestLengthMs }) => {
    const leftMargin = (100 * problemResult.lastSubmitTimeMs / contestLengthMs) * 0.96 + "%";
    return (
        <ProblemWrap leftMargin={leftMargin}>
            <Circle pending={problemResult.pendingAttempts > 0} 
                solved={problemResult.type === ProblemResult.Type.ICPC ? problemResult.isSolved : problemResult.score > 0} />
            <Label>
                {problemResult.type === ProblemResult.Type.ICPC
                    ? `${(problemResult.isSolved ? "+" : problemResult.pendingAttempts > 0 ? "?" : "-") +
                        (problemResult.wrongAttempts + problemResult.pendingAttempts > 0 ? 
                            problemResult.wrongAttempts + problemResult.pendingAttempts : "")}`
                    : `${problemResult.score}`}
            </Label>
        </ProblemWrap>
    );
};


export const TimeLine = ({ className, teamId }) => {
    const problemResults = useAppSelector((state) =>
        state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]?.problemResults);
    const contestLengthMs = useAppSelector(state => state.contestInfo.info?.contestLengthMs);

    return (
        <TimeLineContainer className={className}>
            <Line>
                {problemResults?.filter(obj => obj.type === ProblemResult.Type.ICPC ? obj.isSolved
                    || obj.pendingAttempts + obj.wrongAttempts > 0 : obj.score > 0)?.map((problemResult, index) => (
                    <Problem problemResult={problemResult} index={index} contestLengthMs={contestLengthMs} key={index} />
                ))}
            </Line>
        </TimeLineContainer>
    );
};

export default TimeLine;
