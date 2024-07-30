import React, { useEffect, useState } from "react";
import styled, { keyframes } from "styled-components";
import { useAppSelector } from "../../../redux/hooks";
import c from "../../../config";
import { isShouldUseDarkColor } from "../../../utils/colors";
import { getIOIColor } from "../../../utils/statusInfo";
import { RunResult } from "../../../../../generated/api";

const ChangeProblemAnimation = keyframes`
    0% {
        opacity: 1;
    }
    50% {
        opacity: 0;
    }
    100% {
        opacity: 1;
    }
`;

const ChangeScoreOrVerdictAnimation = keyframes`
    0% {
        opacity: 0;
    }
    50% {
        opacity: 1;
    }
    100% {
        opacity: 0;
    }
`;

const TimeLineContainer = styled.div`
    align-items: center;
    width: 100%;
    border-top-right-radius: ${c.TIMELINE_BORDER_RADIUS};
    border-top-left-radius: ${c.TIMELINE_BORDER_RADIUS};
    border-bottom-right-radius: ${c.TIMELINE_BORDER_RADIUS};;
    display: grid;
    height: ${c.TIMELINE_WRAP_HEIGHT};
    background-color: ${c.CONTEST_COLOR};
`;


const Line = styled.div`
    width: 100%;
    height: ${c.TIMLINE_LINE_HEIGHT};
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
    position: relative;
`;

const Circle = styled.div`
    width: ${c.TIMLINE_CIRCLE_RADIUS};
    height: ${c.TIMLINE_CIRCLE_RADIUS};
    background-color: ${( { color } ) => color };
    border-radius: 50%;
    position: absolute;
    align-content: center;
`;

const Label = styled.div`
    position: relative;
    justify-content: center;
    display: flex;
    align-self: center;
    text-align: center;
    align-items: center;
    color: ${({ darkText }) => darkText ? "#000" : "#FFF"};
    font-weight: ${({ isBold }) => isBold ? "bold" : "normal"};
`;

const ProblemWrap = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: ${({ left }) => left};
`;

const ProblemWithAnimation = styled.div`
    animation: ${ChangeProblemAnimation} 10s infinite;
    justify-content: center;
    position: absolute;
    align-items: center;
    text-align: center;
`;

const ScoreOrVerdictWithAnimation = styled.div`
    animation: ${ChangeScoreOrVerdictAnimation} 10s infinite;
    justify-content: center;
    position: absolute;
    align-items: center;
    text-align: center;
    font-size: 12px;
`;

const Text = styled.div`
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
`;


const getColor = (problemResult, contestInfo) => {
    if (problemResult.type === RunResult.Type.IN_PROGRESS) {
        return c.VERDICT_UNKNOWN;
    } else if (problemResult.type === RunResult.Type.ICPC) {
        if (problemResult.isAccepted) {
            return c.VERDICT_OK;
        } else {
            return c.VERDICT_NOK;
        }
    } else {
        const task = contestInfo.problems.find(info => info.letter === problemResult.problemId);
        return getIOIColor(problemResult.score, task?.minScore, task?.maxScore);
    }
};

export const TimeLine = ({ className, teamId }) => {
    const contestInfo = useAppSelector(state => state.contestInfo.info);
    const [runsResults, setRunsResults] = useState([]);

    const Problem = ({ problemResult, color, contestLengthMs }) => {
        let left = (100 * problemResult.time / contestLengthMs) * 0.99;
        const darkText = isShouldUseDarkColor(color);
        return (
            <ProblemWrap left={left + "%"}>
                <Circle color={getColor(problemResult, contestInfo)}>
                    <Label darkText={darkText} isBold={problemResult.type === RunResult.Type.IN_PROGRESS}>
                        {(problemResult.type === RunResult.Type.IOI || problemResult.type === RunResult.Type.ICPC
                            && !problemResult.isAccepted) && <ProblemWithAnimation>{problemResult.problemId}</ProblemWithAnimation> }
                        {!(problemResult.type === RunResult.Type.IOI || problemResult.type === RunResult.Type.ICPC
                            && !problemResult.isAccepted) && <Text>{problemResult.problemId}</Text>}
                        {problemResult.type === RunResult.Type.ICPC && !problemResult.isAccepted && <ScoreOrVerdictWithAnimation>{problemResult.shortName}</ScoreOrVerdictWithAnimation>}
                        {problemResult.type === RunResult.Type.IOI && <ScoreOrVerdictWithAnimation>{problemResult.score}</ScoreOrVerdictWithAnimation>}
                    </Label>
                </Circle>
            </ProblemWrap>
        );
    };

    useEffect(() => {
        const socket = new WebSocket(c.BASE_URL_WS + "/teamRuns/" + teamId);
        socket.onopen = function () {
            console.debug(`WebSocket /teamRuns/${teamId} is open`);
        };

        socket.onmessage = function (event) {
            const obj = JSON.parse(event.data);
            setRunsResults(obj);
            console.debug(`WebSocket /teamRuns/${teamId}: ` + obj);
        };

        socket.onclose = function() {
            console.debug(`WebSocket /teamRuns/${teamId} is closed`);
        };

        socket.onerror = function(error) {
            console.log(`WebSocket /teamRuns/${teamId} error: ` + error);
        };

        return () => {
            socket.close();
        };
    }, [teamId]);


    return (
        <TimeLineContainer className={className}>
            <Line>
                {runsResults?.map((problemResult, index) => (
                    <Problem problemResult={problemResult} contestLengthMs={contestInfo?.contestLengthMs} key={index} />
                ))}
            </Line>
        </TimeLineContainer>
    );
};

export default TimeLine;
