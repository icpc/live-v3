import React, { useEffect, useState } from "react";
import styled from "styled-components";
import { useAppSelector } from "../../../redux/hooks";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ProblemResult } from "../../../../../generated/api";
import c from "../../../config";
import { isShouldUseDarkColor } from "../../../utils/colors";

const TimeLineContainer = styled.div`
    align-items: center;
    width: 100%;
    border-top-left-radius: 20px;
    border-top-right-radius: 20px;
    display: grid; 
    height: 60px;
    background-color: ${c.CONTEST_COLOR};
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
    justify-content: center;
    align-items: center;
    text-align: center;
    align-content: center;
`;

const Label = styled.div`
    position: relative;
    align-self: center;
    align-items: center;
    justify-content: center;
    display: flex;
    text-align: center;
`;

const ProblemWrap = styled.div`
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: ${props => props.leftMargin};
`;

const Problem = ({ problemResult, letter, color, contestLengthMs }) => {
    const leftMargin = (100 * problemResult.time / contestLengthMs) * 0.99 + "%";
    console.log(letter, color);
    const dark = isShouldUseDarkColor(color);
    console.log(problemResult);
    const verdict = problemResult?.result?.verdict;
    console.log(problemResult.result.type === ProblemResult.Type.ICPC ? verdict?.isAccepted : problemResult.score > 0);
    return (
        <ProblemWrap leftMargin={leftMargin}>
            <Circle pending={!verdict?.isAccepted && !verdict?.isAddingPenalty}
                solved={problemResult.result.type === ProblemResult.Type.ICPC ? verdict?.isAccepted : problemResult.score > 0}>
                <Label>
                    {letter}
                </Label>
            </Circle>
        </ProblemWrap>
    );
};


export const TimeLine = ({ className, teamId }) => {
    const contestLengthMs = useAppSelector(state => state.contestInfo.info?.contestLengthMs);
    const [runsResults, setRunsResults] = useState([]);
    
    const getResultRuns = (runs) => {
        return runs.filter(obj => obj.teamId === teamId);
    };
    
    const getRuns = () => {
        fetch(c.RUNS_URL).then(response => response.json())
            .then(response => setRunsResults(getResultRuns(response)));
    };

    useEffect(() => {
        getRuns();
    }, []);
    
    return (
        <TimeLineContainer className={className}>
            <Line>
                {runsResults?.map((problemResult, index) => (
                    <Problem problemResult={problemResult} letter={problemResult.problemId}
                        contestLengthMs={contestLengthMs} key={index} />
                ))}
            </Line>
        </TimeLineContainer>
    );
};

export default TimeLine;
