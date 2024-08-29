import React, { useEffect, useState } from "react";
import styled, { keyframes } from "styled-components";
import { useAppSelector } from "@/redux/hooks";
import c from "@/config";
import { getIOIColor } from "@/utils/statusInfo";
import { RunResult } from "@shared/api";
import { getStartTime } from "@/components/molecules/Clock";

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
    height: ${c.TIMELINE_WRAP_HEIGHT + "px"};
    background-color: ${c.CONTEST_COLOR};
    position: relative;
`;

interface LineProps {
    lineWidth: number;
}


const Line = styled.div.attrs<LineProps>(({ lineWidth }) => ({
    style: {
        width: `${lineWidth}%`,
    },
}))<LineProps>`
    height: ${c.TIMLINE_LINE_HEIGHT};
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
    position: absolute;
`;

interface CircleAtEndProps {
    lineWidth: number;
}

const CircleAtEnd = styled.div.attrs<CircleAtEndProps>(({ lineWidth }) => ({
    style: {
        left: `${lineWidth}%`,
    },
}))<CircleAtEndProps>`
    width: 10px;
    height: 10px;
    border-radius: 50%;
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
`;

const Circle = styled.div`
    width: ${c.TIMLINE_CIRCLE_RADIUS + "px"};
    height: ${c.TIMLINE_CIRCLE_RADIUS + "px"};
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
    color: white;
    font-weight: bold;
`;

const ProblemWrap = styled.div<{ left: string, top: string }>`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: ${({ left }) => left};
    top: ${({ top }) => top};
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
`;

const Text = styled.div`
    position: absolute;
    justify-content: center;
    align-items: center;
    text-align: center;
`;

const TimeBorder = styled.div<{ left: string, last: boolean }>`
    height: ${({ last }) => last ? c.TIMELINE_WRAP_HEIGHT + "px" : c.TIMELINE_WRAP_HEIGHT + "px"};
    background-color: ${c.TIMELINE_TIMEBORDER_COLOR};
    width: 2px;
    position: absolute;
    left: ${({ left }) => left};
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

const Problem = ({ problemResult, contestInfo, animationKey }) => {
    const contestLengthMs = contestInfo?.contestLengthMs;
    const problemNumber = contestInfo?.problems.findIndex(elem => elem.id === problemResult.problemId);
    const problemsCount = contestInfo?.problems.length;
    const top = (c.TIMELINE_WRAP_HEIGHT - c.TIMLINE_CIRCLE_RADIUS ) / problemsCount * problemNumber + c.TIMLINE_CIRCLE_RADIUS / 2;
    const left = (100 * problemResult.time / contestLengthMs) * 0.983;
    const color = getColor(problemResult, contestInfo);

    return (
        <ProblemWrap left={left + "%"} top={top + "px"} key={animationKey}>
            <Circle color={color} />
            <Label>
                {(problemResult.type === RunResult.Type.IOI || problemResult.type === RunResult.Type.ICPC
                    && !problemResult.isAccepted) 
                    && <ProblemWithAnimation>{problemResult.problemId}</ProblemWithAnimation> }
                {!(problemResult.type === RunResult.Type.IOI || problemResult.type === RunResult.Type.ICPC
                    && !problemResult.isAccepted) && <Text>{problemResult.problemId}</Text>}
                {problemResult.type === RunResult.Type.ICPC && !problemResult.isAccepted
                    && <ScoreOrVerdictWithAnimation>{problemResult.shortName}</ScoreOrVerdictWithAnimation>}
                {problemResult.type === RunResult.Type.IOI 
                    && <ScoreOrVerdictWithAnimation>{problemResult.score}</ScoreOrVerdictWithAnimation>}
            </Label>
        </ProblemWrap>
    );
};

export const TimeLine = ({ teamId, className = null }) => {
    const contestInfo = useAppSelector(state => state.contestInfo.info);
    const [runsResults, setRunsResults] = useState([]);
    const [animationKey, setAnimationKey] = useState(0);
    const [lineWidth, setLineWidth] = useState(100);

    useEffect(() => {
        const socket = new WebSocket(c.BASE_URL_WS + "/teamRuns/" + teamId);
        socket.onopen = function () {
            console.debug(`WebSocket /teamRuns/${teamId} is open`);
        };

        socket.onmessage = function (event) {
            const obj = JSON.parse(event.data);
            setRunsResults(obj);
            setAnimationKey(prev => prev + 1);
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

    useEffect(() => {
        const updateLineWidth = () => {
            if (!contestInfo) return;
            const currentTime = Date.now();
            const elapsedTime = currentTime - getStartTime(contestInfo?.status);
            const progress = Math.min(100, elapsedTime / contestInfo?.contestLengthMs * 1000);
            setLineWidth(progress * 0.983);
        };

        const interval = setInterval(updateLineWidth, 1000);

        return () => {
            clearInterval(interval);
        };
    }, [contestInfo]);


    return (
        <TimeLineContainer className={className}>
            <Line lineWidth={lineWidth} />
            <CircleAtEnd lineWidth={lineWidth}/>
            {Array.from(Array((contestInfo?.contestLengthMs ?? 0) / 3600000).keys()).map(elem =>
                <TimeBorder key={elem} left={((elem + 1) * 3600000 / contestInfo.contestLengthMs * 100) * 0.983 + "%"}
                    last={elem === contestInfo.contestLengthMs / 3600000 - 1} />)}
            {runsResults?.map((problemResult, index) => (
                <Problem problemResult={problemResult} contestInfo={contestInfo} key={`${animationKey}-${index}`}
                    animationKey={animationKey}/>
            ))}
        </TimeLineContainer>
    );
};

export default TimeLine;
