import { useEffect, useState, useRef } from "react";
import styled from "styled-components";
import { useAppSelector } from "@/redux/hooks";
import c from "@/config";
import { getIOIColor } from "@/utils/statusInfo";
import { RunResult } from "@shared/api";
import { getStartTime } from "@/components/molecules/Clock";
import { DateTime } from "luxon";
import { isShouldUseDarkColor } from "@/utils/colors";

interface TimeLineContainerProps {
    isPvp: boolean;
}

const TimeLineContainer = styled.div.attrs<TimeLineContainerProps>(({ isPvp }) => ({
    style: {
        height: `${isPvp ? c.TIMELINE_WRAP_HEIGHT_PVP : c.TIMELINE_WRAP_HEIGHT}px`,
    }
})) <TimeLineContainerProps>`
    align-items: center;
    width: 100%;
    display: grid;
    background-color: ${props => props.color};
    position: relative;
    border-bottom-left-radius: ${c.TIMELINE_BORDER_RADIUS};
    border-top-left-radius: ${c.TIMELINE_BORDER_RADIUS};
`;

interface LineProps {
    lineWidth: number;
    left: number;
}

const Line = styled.div.attrs<LineProps>(({ lineWidth, left }) => ({
    style: {
        width: `${lineWidth}%`,
        left: `${left}px`,
    },
})) <LineProps>`
    height: ${c.TIMELINE_LINE_HEIGHT}px;
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
    position: absolute;
`;

interface CircleAtEndProps {
    lineWidth: number;
    leftPadding: number;
}

const CircleAtEnd = styled.div.attrs<CircleAtEndProps>(({ lineWidth, leftPadding }) => ({
    style: {
        left: `calc(${lineWidth}% + ${leftPadding}px)`,
    },
})) <CircleAtEndProps>`
    width: 10px;
    height: 10px;
    border-radius: 50%;
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    background: linear-gradient(270deg, #D13D23 -28.28%, #FFC239 33.33%, #1A63D8 100%);
`;

interface CircleProps {
    isPvp: boolean;
}

const Circle = styled.div.attrs<CircleProps>(({ isPvp }) => ({
    style: {
        width: `${isPvp ? c.TIMELINE_ELEMENT_DIAMETER_PVP : c.TIMELINE_ELEMENT_DIAMETER}px`,
        height: `${isPvp ? c.TIMELINE_ELEMENT_DIAMETER_PVP : c.TIMELINE_ELEMENT_DIAMETER}px`,
    }
})) <CircleProps>`
    border-radius: 50%;
    position: absolute;
    align-content: center;
    background-color: ${({ color }) => color};
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
    justify-content: center;
    position: absolute;
    align-items: center;
    text-align: center;
`;

const ScoreOrVerdictWithAnimation = styled.div`
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

const TimeBorder = styled.div<{ left: string, color: string, isPvp: boolean }>`
    height: ${props => props.isPvp ? c.TIMELINE_WRAP_HEIGHT_PVP : c.TIMELINE_WRAP_HEIGHT}px;
    background-color: ${({ color }) => isShouldUseDarkColor(color) ? "#000" : "#fff"};
    width: 2px;
    position: absolute;
    left: ${({ left }) => left};
`;

const getProblemPosition = (problemNumber, problemsCount, isPvp) => {
    const config = isPvp ? {
        wrapHeight: c.TIMELINE_WRAP_HEIGHT_PVP,
        padding: c.TIMELINE_PADDING_PVP,
        diameter: c.TIMELINE_ELEMENT_DIAMETER_PVP
    } : {
        wrapHeight: c.TIMELINE_WRAP_HEIGHT,
        padding: c.TIMELINE_PADDING,
        diameter: c.TIMELINE_ELEMENT_DIAMETER
    };

    const height = Math.max(config.wrapHeight - 2 * config.padding - config.diameter, 0);
    const top = height / (problemsCount - 1) * problemNumber + config.padding + config.diameter / 2;

    return { top };
};

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
        const task = contestInfo.problems.find(info => info.id === problemResult.problemId);
        return getIOIColor(problemResult.score, task?.minScore, task?.maxScore);
    }
};

const Problem = ({ problemResult, contestInfo, syncStartTime, isPvp = false }) => {
    const problemLetterRef = useRef(null);
    const scoreVerdictRef = useRef(null);
    const contestLengthMs = contestInfo?.contestLengthMs;
    const problemNumber = contestInfo?.problems.findIndex(elem => elem.id === problemResult.problemId);
    const problemsCount = contestInfo?.problems.length;

    const { top } = getProblemPosition(problemNumber, problemsCount, isPvp);
    const left = (100 * problemResult.time / contestLengthMs) * (isPvp ? c.TIMELINE_REAL_WIDTH_PVP : c.TIMELINE_REAL_WIDTH);
    const leftInPx = `calc(${left}% + ${c.TIMELINE_LEFT_TIME_PADDING}px)`;
    const color = getColor(problemResult, contestInfo);
    const letter = useAppSelector((state) => state.contestInfo.info?.problemsId[problemResult.problemId].letter);

    const shouldAnimateProblem = !isPvp && problemResult.type === RunResult.Type.IOI ||
        !isPvp && (problemResult.type === RunResult.Type.ICPC && !problemResult.isAccepted);
    const shouldAnimateScoreVerdict = !isPvp && problemResult.type === RunResult.Type.IOI ||
        !isPvp && (problemResult.type === RunResult.Type.ICPC && !problemResult.isAccepted);

    useEffect(() => {
        if (shouldAnimateProblem && problemLetterRef.current && syncStartTime) {
            const animation = problemLetterRef.current.animate([
                { opacity: 1 },
                { opacity: 0 },
                { opacity: 1 }
            ], {
                duration: c.TIMELINE_ANIMATION_TIME,
                iterations: Infinity
            });
            animation.startTime = syncStartTime;
        }
    }, [shouldAnimateProblem, syncStartTime]);

    useEffect(() => {
        if (shouldAnimateScoreVerdict && scoreVerdictRef.current && syncStartTime) {
            const animation = scoreVerdictRef.current.animate([
                { opacity: 0 },
                { opacity: 1 },
                { opacity: 0 }
            ], {
                duration: c.TIMELINE_ANIMATION_TIME,
                iterations: Infinity
            });
            animation.startTime = syncStartTime;
        }
    }, [shouldAnimateScoreVerdict, syncStartTime]);

    return (
        <ProblemWrap left={leftInPx} top={top + "px"}>
            <Circle color={color} isPvp={isPvp} />
            <Label>
                {shouldAnimateProblem && (
                    <ProblemWithAnimation ref={problemLetterRef}>{letter}</ProblemWithAnimation>
                )}
                {!shouldAnimateProblem && <Text>{letter}</Text>}
                {(!isPvp && problemResult.type === RunResult.Type.ICPC) && !problemResult.isAccepted && (
                    <ScoreOrVerdictWithAnimation ref={scoreVerdictRef}>
                        {problemResult.shortName}
                    </ScoreOrVerdictWithAnimation>
                )}
                {(!isPvp && problemResult.type === RunResult.Type.IOI) && (
                    <ScoreOrVerdictWithAnimation ref={scoreVerdictRef}>
                        {Math.round(problemResult.score * 100) / 100}
                    </ScoreOrVerdictWithAnimation>
                )}
            </Label>
        </ProblemWrap>
    );
};

const TimelineBackground = styled.div`
    background-color: ${props => props.color};
    grid-column: 2 / 2;
    grid-row: 4 / 4;
    border-bottom-right-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

export const TimeLineBackground = ({ teamId, classname = null }) => {
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[teamId]);

    return <TimelineBackground className={classname} color={teamData?.color ? teamData?.color : c.CONTEST_COLOR} />;
};

export const TimeLine = ({ teamId, className = null, isPvp = false }) => {
    const contestInfo = useAppSelector(state => state.contestInfo.info);
    const [runsResults, setRunsResults] = useState([]);
    const [syncStartTime, setSyncStartTime] = useState(null);
    const [lineWidth, setLineWidth] = useState(0);

    useEffect(() => {
        const socket = new WebSocket(c.BASE_URL_WS + "/teamRuns/" + teamId);
        socket.onopen = function () {
            console.debug(`WebSocket /teamRuns/${teamId} is open`);
        };

        socket.onmessage = function (event) {
            const obj = JSON.parse(event.data);
            setRunsResults(obj);

            setSyncStartTime(prevTime => prevTime || performance.now());

            console.debug(`WebSocket /teamRuns/${teamId}: ` + obj);
        };

        socket.onclose = function () {
            console.debug(`WebSocket /teamRuns/${teamId} is closed`);
        };

        socket.onerror = function (error) {
            console.log(`WebSocket /teamRuns/${teamId} error: `, error);
        };

        return () => {
            socket.close();
        };
    }, [teamId]);

    useEffect(() => {
        const updateLineWidth = () => {
            if (!contestInfo) return;
            const elapsedTime = DateTime.fromMillis(getStartTime(contestInfo.status)).diffNow().negate().milliseconds;
            const progress = Math.min(100, elapsedTime / contestInfo?.contestLengthMs * 100 * (contestInfo.emulationSpeed ?? 1));
            setLineWidth(progress * (isPvp ? c.TIMELINE_REAL_WIDTH_PVP : c.TIMELINE_REAL_WIDTH));
        };

        const interval = setInterval(updateLineWidth, 1000);

        return () => {
            clearInterval(interval);
        };
    }, [contestInfo]);

    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[teamId]);

    return (
        <TimeLineContainer
            className={className}
            color={teamData?.color ? teamData?.color : c.CONTEST_COLOR}
            isPvp={isPvp}
        >
            <Line lineWidth={lineWidth} left={c.TIMELINE_LEFT_TIME_PADDING}/>
            <CircleAtEnd lineWidth={lineWidth} leftPadding={c.TIMELINE_LEFT_TIME_PADDING}/>
            {Array.from(Array((Math.floor((contestInfo?.contestLengthMs ?? 0) / 3600000) + 1)).keys()).map(elem => {
                return (
                    <TimeBorder
                        key={elem}
                        color={teamData?.color ?? "#000"}
                        left={`calc(${((elem) * 3600000 / contestInfo?.contestLengthMs * 100) * (isPvp ? c.TIMELINE_REAL_WIDTH_PVP
                            : c.TIMELINE_REAL_WIDTH)}% + ${c.TIMELINE_LEFT_TIME_PADDING}px)`}
                        isPvp={isPvp}
                    />
                );
            })}
            {runsResults?.map((problemResult, index) => (
                <Problem
                    key={`${problemResult.problemId}-${problemResult.time}-${index}`}
                    problemResult={problemResult}
                    contestInfo={contestInfo}
                    syncStartTime={syncStartTime}
                    isPvp={isPvp}
                />
            ))}
        </TimeLineContainer>
    );
};

export default TimeLine;
