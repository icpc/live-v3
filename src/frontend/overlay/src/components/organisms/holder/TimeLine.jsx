import React, { useEffect, useState } from "react";
import styled from "styled-components";
import { useAppSelector } from "../../../redux/hooks";
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
    color: ${({ darkText }) => darkText ? "#000" : "#FFF"};
`;

const ProblemWrap = styled.div`
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: ${({ left }) => left};;
`;

const Problem = ({ problemResult, color, contestLengthMs }) => {
    const left = (100 * problemResult.time / contestLengthMs) * 0.99 + "%";
    const darkText = isShouldUseDarkColor(color);
    return (
        <ProblemWrap left={left}>
            <Circle pending={problemResult.type === "IN_PROGRESS"}
                solved={problemResult.type === "ICPC" ? problemResult.isAccepted : problemResult.score > 0}>
                <Label darkText={darkText}>
                    {problemResult.problemId}
                </Label>
            </Circle>
        </ProblemWrap>
    );
};


export const TimeLine = ({ className, teamId }) => {
    const contestLengthMs = useAppSelector(state => state.contestInfo.info?.contestLengthMs);
    const [runsResults, setRunsResults] = useState([]);

    useEffect(() => {
        const socket = new WebSocket(c.BASE_URL_WS + "/teamRuns");
        socket.onopen = function () {
            console.log("WebSocket is open");
            socket.send(teamId);
        };

        socket.onmessage = function (event) {
            setRunsResults(JSON.parse(event.data));
            console.log(event.data);
        };

        socket.onclose = function() {
            console.log("WebSocket is closed");
        };

        socket.onerror = function(error) {
            console.log("WebSocket error: " + error);
        };

        return () => {
            socket.close();
        };
    }, [teamId]);


    return (
        <TimeLineContainer className={className}>
            <Line>
                {runsResults?.map((problemResult, index) => (
                    <Problem problemResult={problemResult} contestLengthMs={contestLengthMs} key={index} />
                ))}
            </Line>
        </TimeLineContainer>
    );
};

export default TimeLine;
