import { useEffect, useState, useRef } from "react";
import styled from "styled-components";
import { useAppSelector } from "@/redux/hooks";
import c from "@/config";
import { getIOIColor } from "@/utils/statusInfo";
import { ContestInfo, TeamId, TeamInfo, TimeLineRunInfo } from "@shared/api";
import {
    calculateContestTime,
    getStartTime,
} from "@/components/molecules/Clock";
import { isShouldUseDarkColor } from "@/utils/colors";
import { KeylogGraph } from "./KeylogGraph";

interface TimeLineContainerProps {
    isPvp: boolean;
    color?: string;
}

interface LineProps {
    lineWidth: number;
    left: number;
}

interface CircleAtEndProps {
    lineWidth: number;
    leftPadding: number;
}

interface CircleProps {
    isPvp: boolean;
    color?: string;
}

interface ProblemWrapProps {
    left: string;
    top: string;
}

interface TimeBorderProps {
    left: string;
    color: string;
    isPvp: boolean;
}

interface ProblemPosition {
    top: number;
}

interface ProblemProps {
    problemResult: TimeLineRunInfo;
    contestInfo: ContestInfo;
    syncStartTime: number | null;
    config: TimelineConfig;
}

interface TimeLineBackgroundProps {
    teamId: TeamId;
    classname?: string | null;
}

interface TimeLineProps {
    teamId: TeamId;
    className?: string | null;
    isPvp?: boolean;
}

interface TimelineConfig {
    wrapHeight: number;
    padding: number;
    diameter: number;
    realWidth: number;
    isPvp: boolean;
}

const TimeLineContainer = styled.div.attrs<TimeLineContainerProps>(
    ({ isPvp }) => ({
        style: {
            height: `${isPvp ? c.TIMELINE_WRAP_HEIGHT_PVP : c.TIMELINE_WRAP_HEIGHT}px`,
        },
    }),
)<TimeLineContainerProps>`
    align-items: center;
    width: 100%;
    display: grid;
    background-color: ${(props) => props.color};
    position: relative;
    border-bottom-left-radius: ${c.TIMELINE_BORDER_RADIUS}px;
    border-top-left-radius: ${c.TIMELINE_BORDER_RADIUS}px;
`;

const Line = styled.div.attrs<LineProps>(({ lineWidth, left }) => ({
    style: {
        width: `${lineWidth}%`,
        left: `${left}px`,
    },
}))<LineProps>`
    height: ${c.TIMELINE_LINE_HEIGHT}px;
    background: linear-gradient(
        270deg,
        #d13d23 -28.28%,
        #ffc239 33.33%,
        #1a63d8 100%
    );
    position: absolute;
`;

const CircleAtEnd = styled.div.attrs<CircleAtEndProps>(
    ({ lineWidth, leftPadding }) => ({
        style: {
            left: `calc(${lineWidth}% + ${leftPadding}px)`,
        },
    }),
)<CircleAtEndProps>`
    width: ${c.TIMELINE_END_CIRCLE_RADIUS}px;
    height: ${c.TIMELINE_END_CIRCLE_RADIUS}px;
    border-radius: 50%;
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    background: linear-gradient(
        270deg,
        #d13d23 -28.28%,
        #ffc239 33.33%,
        #1a63d8 100%
    );
`;

const Circle = styled.div.attrs<CircleProps>(({ isPvp }) => ({
    style: {
        width: `${isPvp ? c.TIMELINE_ELEMENT_DIAMETER_PVP : c.TIMELINE_ELEMENT_DIAMETER}px`,
        height: `${isPvp ? c.TIMELINE_ELEMENT_DIAMETER_PVP : c.TIMELINE_ELEMENT_DIAMETER}px`,
    },
}))<CircleProps>`
    border-radius: 50%;
    position: absolute;
    align-content: center;
    background-color: ${(props) => props.color};
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

const ProblemWrap = styled.div<ProblemWrapProps>`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: ${(props) => props.left};
    top: ${(props) => props.top};
`;

const AnimatedText = styled.div`
    justify-content: center;
    position: absolute;
    align-items: center;
    text-align: center;
`;

const StaticText = styled.div`
    position: absolute;
    justify-content: center;
    align-items: center;
    text-align: center;
`;

const TimeBorder = styled.div<TimeBorderProps>`
    height: ${(props) =>
        props.isPvp ? c.TIMELINE_WRAP_HEIGHT_PVP : c.TIMELINE_WRAP_HEIGHT}px;
    background-color: ${(props) =>
        isShouldUseDarkColor(props.color) ? "#000" : "#fff"};
    width: ${c.TIMELINE_TIME_BORDER_WIDTH}px;
    position: absolute;
    left: ${(props) => props.left};
`;

const TimelineBackground = styled.div<{ color: string }>`
    background-color: ${(props) => props.color};
    grid-column: 2 / 2;
    grid-row: 4 / 4;
    border-bottom-right-radius: ${c.GLOBAL_BORDER_RADIUS}px;
`;

function getTimelineConfig(isPvp: boolean): TimelineConfig {
    return isPvp
        ? {
              wrapHeight: c.TIMELINE_WRAP_HEIGHT_PVP,
              padding: c.TIMELINE_PADDING_PVP,
              diameter: c.TIMELINE_ELEMENT_DIAMETER_PVP,
              realWidth: c.TIMELINE_REAL_WIDTH_PVP,
              isPvp: true,
          }
        : {
              wrapHeight: c.TIMELINE_WRAP_HEIGHT,
              padding: c.TIMELINE_PADDING,
              diameter: c.TIMELINE_ELEMENT_DIAMETER,
              realWidth: c.TIMELINE_REAL_WIDTH,
              isPvp: false,
          };
}

function calculateProblemPosition(
    problemNumber: number,
    problemsCount: number,
    config: TimelineConfig,
): ProblemPosition {
    if (problemsCount <= 1) {
        return { top: 0 };
    }

    const availableHeight = Math.max(
        config.wrapHeight - 2 * config.padding - config.diameter,
        0,
    );

    const spacing = availableHeight / (problemsCount - 1);
    const top = spacing * problemNumber + config.padding + config.diameter / 2;

    return { top };
}

function calculateProblemLeftPotision(
    time: number,
    contestLengthMs: number,
    config: TimelineConfig,
): string {
    const percentage = ((100 * time) / contestLengthMs) * config.realWidth;
    return `calc(${percentage}% + ${c.TIMELINE_LEFT_TIME_PADDING}px)`;
}

function getProblemColor(
    problemResult: TimeLineRunInfo,
    contestInfo: ContestInfo,
): string {
    switch (problemResult.type) {
        case TimeLineRunInfo.Type.IN_PROGRESS:
            return c.VERDICT_UNKNOWN;
        case TimeLineRunInfo.Type.ICPC:
            return problemResult.isAccepted ? c.VERDICT_OK : c.VERDICT_NOK;
        case TimeLineRunInfo.Type.IOI:
            const problem = contestInfo.problems.find(
                (p) => p.id === problemResult.problemId,
            );
            return getIOIColor(
                problemResult.score,
                problem?.minScore ?? null,
                problem?.maxScore ?? null,
            );
        default:
            return c.VERDICT_UNKNOWN;
    }
}

function shouldAnimateProblem(
    problemResult: TimeLineRunInfo,
    config: TimelineConfig,
): boolean {
    if (config.isPvp) {
        return false;
    }

    return (
        problemResult.type === TimeLineRunInfo.Type.IOI ||
        (problemResult.type === TimeLineRunInfo.Type.ICPC &&
            !problemResult.isAccepted)
    );
}

function createAnimation(
    element: HTMLElement,
    keyframes: Keyframe[],
    syncStartTime: number,
): Animation {
    const animation = element.animate(keyframes, {
        duration: c.TIMELINE_ANIMATION_TIME,
        iterations: Infinity,
    });
    animation.startTime = syncStartTime;
    return animation;
}

function extractKeylogUrl(teamData: TeamInfo | undefined): string | null {
    const keylogs = teamData?.medias?.keylog;
    if (!keylogs || keylogs.length === 0) return null;
    const url = keylogs[0]?.url;
    return url ?? null;
}

function generateHourMarkers(contestLengthMs: number): number[] {
    const hours = Math.floor(contestLengthMs / 3600000) + 1;
    return Array.from({ length: hours }, (_, i) => i);
}

function calculateHourMarkerPosition(
    hour: number,
    contestLengthMs: number,
    config: TimelineConfig,
): string {
    const percentage =
        ((hour * 3600000) / contestLengthMs) * 100 * config.realWidth;
    return `calc(${percentage}% + ${c.TIMELINE_LEFT_TIME_PADDING}px)`;
}

function Problem({
    problemResult,
    contestInfo,
    syncStartTime,
    config,
}: ProblemProps) {
    const problemLetterRef = useRef<HTMLDivElement>(null);
    const scoreVerdictRef = useRef<HTMLDivElement>(null);

    const problemIndex = contestInfo.problems.findIndex(
        (p) => p.id === problemResult.problemId,
    );
    const problemLetter = useAppSelector(
        (state) =>
            state.contestInfo.info?.problemsId[problemResult.problemId]?.letter,
    );

    const position = calculateProblemPosition(
        problemIndex,
        contestInfo.problems.length,
        config,
    );
    const leftPosition = calculateProblemLeftPotision(
        problemResult.time,
        contestInfo.contestLengthMs,
        config,
    );

    const color = getProblemColor(problemResult, contestInfo);
    const shouldAnimate = shouldAnimateProblem(problemResult, config);

    useEffect(() => {
        if (!shouldAnimate || !syncStartTime) return;

        if (problemLetterRef.current) {
            createAnimation(
                problemLetterRef.current,
                [{ opacity: 1 }, { opacity: 0 }, { opacity: 1 }],
                syncStartTime,
            );
        }
    }, [shouldAnimate, syncStartTime]);

    useEffect(() => {
        if (!shouldAnimate || !syncStartTime) return;

        if (scoreVerdictRef.current) {
            createAnimation(
                scoreVerdictRef.current,
                [{ opacity: 0 }, { opacity: 1 }, { opacity: 0 }],
                syncStartTime,
            );
        }
    }, [shouldAnimate, syncStartTime]);

    function renderProblemLabel() {
        if (shouldAnimate) {
            return (
                <AnimatedText ref={problemLetterRef}>
                    {problemLetter}
                </AnimatedText>
            );
        }
        return <StaticText>{problemLetter}</StaticText>;
    }

    function renderScoreOrVerdict() {
        if (config.isPvp) return null;

        if (
            problemResult.type === TimeLineRunInfo.Type.ICPC &&
            !problemResult.isAccepted
        ) {
            return (
                <AnimatedText ref={scoreVerdictRef}>
                    {problemResult.shortName}
                </AnimatedText>
            );
        }

        if (problemResult.type === TimeLineRunInfo.Type.IOI) {
            const roundedScore = Math.round(problemResult.score * 100) / 100;
            return (
                <AnimatedText ref={scoreVerdictRef}>
                    {roundedScore}
                </AnimatedText>
            );
        }

        return null;
    }

    return (
        <ProblemWrap left={leftPosition} top={`${position.top}px`}>
            <Circle color={color} isPvp={config.isPvp} />
            <Label>
                {renderProblemLabel()}
                {renderScoreOrVerdict()}
            </Label>
        </ProblemWrap>
    );
}

export function TimeLineBackground({
    teamId,
    classname = null,
}: TimeLineBackgroundProps) {
    const teamData = useAppSelector(
        (state) => state.contestInfo.info?.teamsId[teamId],
    ) as TeamInfo | undefined;

    return (
        <TimelineBackground
            className={classname ?? undefined}
            color={teamData?.color ?? c.CONTEST_COLOR}
        />
    );
}

interface KeyboardEvent {
    timestamp: string; // ISO8601
    keys: Record<string, KeyStats>;
}

interface KeyStats {
    raw?: number;
    bare?: number;
    shift?: number;
    ctrl?: number;
    alt?: number;
    meta?: number;

    "ctrl+shift"?: number;
    "ctrl+alt"?: number;
    "shift+alt"?: number;
    "ctrl+meta"?: number;
    "shift+meta"?: number;
    "alt+meta"?: number;

    "ctrl+shift+alt"?: number;
    "ctrl+shift+meta"?: number;
    "ctrl+alt+meta"?: number;
    "shift+alt+meta"?: number;

    "ctrl+shift+alt+meta"?: number;
}

export function TimeLine({
    teamId,
    className = null,
    isPvp = false,
}: TimeLineProps) {
    const contestInfo = useAppSelector((state) => state.contestInfo.info) as
        | ContestInfo
        | undefined;
    const teamData = useAppSelector(
        (state) => state.contestInfo.info?.teamsId[teamId],
    ) as TeamInfo | undefined;

    const [runsResults, setRunsResults] = useState<TimeLineRunInfo[]>([]);
    const [syncStartTime, setSyncStartTime] = useState<number | null>(null);
    const [lineWidth, setLineWidth] = useState<number>(0);
    const [keylog, setKeylog] = useState<number[]>([]);

    const teamColor = teamData?.color ?? c.CONTEST_COLOR;
    const keylogUrl = extractKeylogUrl(teamData);

    const config = getTimelineConfig(isPvp);

    useEffect(() => {
        const socketUrl = `${c.BASE_URL_WS}/teamRuns/${teamId}`;
        const socket = new WebSocket(socketUrl);

        function handleOpen() {
            console.debug(`WebSocket /teamRuns/${teamId} is open`);
        }

        function handleMessage(event: MessageEvent) {
            try {
                const data: TimeLineRunInfo[] = JSON.parse(event.data);
                setRunsResults(data);
                setSyncStartTime((prevTime) => prevTime || performance.now());
                console.debug(`WebSocket /teamRuns/${teamId}: `, data);
            } catch (error) {
                console.error(
                    `Error parsing WebSocket message for /teamRuns/${teamId}:`,
                    error,
                );
            }
        }

        function handleClose() {
            console.debug(`WebSocket /teamRuns/${teamId} is closed`);
        }

        function handleError(error: Event) {
            console.error(`WebSocket /teamRuns/${teamId} error:`, error);
        }

        socket.addEventListener("open", handleOpen);
        socket.addEventListener("message", handleMessage);
        socket.addEventListener("close", handleClose);
        socket.addEventListener("error", handleError);

        return () => {
            socket.close();
        };
    }, [teamId]);

    useEffect(() => {
        if (!contestInfo) return;

        function updateProgress() {
            const elapsedTime = calculateContestTime(contestInfo);
            const progressPercentage = Math.min(
                100,
                (elapsedTime / contestInfo.contestLengthMs) * 100,
            );
            const config = getTimelineConfig(isPvp);
            setLineWidth(progressPercentage * config.realWidth);
        }

        updateProgress();
        const interval = setInterval(updateProgress, 1000);

        return () => {
            clearInterval(interval);
        };
    }, [contestInfo, isPvp]);

    useEffect(() => {
        const startTime = getStartTime(contestInfo);
        if (!keylogUrl || !startTime) return;

        // TODO: Move all this code to KeylogGraph
        async function fetchNDJSON(): Promise<KeyboardEvent[]> {
            try {
                const response = await fetch(keylogUrl);
                if (!response.ok) throw new Error("Failed to fetch keylog");

                const text = await response.text();
                return text
                    .trim()
                    .split("\n")
                    .filter((line) => line.trim())
                    .map((line) => JSON.parse(line) as KeyboardEvent);
            } catch (e) {
                console.error(e);
                return [];
            }
        }

        async function fetchKeylogData() {
            const events = await fetchNDJSON();
            if (events.length === 0) return;

            const contestStart = new Date(startTime!).getTime();
            const AGGREGATION_MS = c.KEYLOG_INTERVAL_LENGTH;

            const totalIntervals = Math.ceil(
                contestInfo!.contestLengthMs / c.KEYLOG_INTERVAL_LENGTH,
            );
            const interval_minutes = c.KEYLOG_INTERVAL_LENGTH / 60000;
            const pressesPerMinuteFactor = 1 / interval_minutes;
            const newKeylog = new Array(totalIntervals).fill(0);

            events.forEach((event) => {
                const eventTime = new Date(event.timestamp).getTime();

                if (eventTime < contestStart) return;

                const timeDiff = eventTime - contestStart;
                const index = Math.floor(timeDiff / AGGREGATION_MS);

                if (index >= 0 && index < totalIntervals) {
                    const pressesCount = Object.values(event.keys).reduce(
                        (sum, k) => sum + (k.bare ?? 0) + (k.shift ?? 0),
                        0,
                    );
                    newKeylog[index] += pressesCount;
                }
            });

            setKeylog(newKeylog.map((v) => v * pressesPerMinuteFactor));
        }

        fetchKeylogData();
    }, [keylogUrl, contestInfo]);

    if (!contestInfo) return null;

    const hourMarkers = generateHourMarkers(contestInfo.contestLengthMs);

    return (
        <TimeLineContainer
            className={className}
            color={teamData?.color ? teamData?.color : c.CONTEST_COLOR}
            isPvp={isPvp}
        >
            {keylog.length > 0 && (
                <KeylogGraph
                    keylog={keylog}
                    isPvp={isPvp}
                    teamColor={teamColor}
                />
            )}

            <Line lineWidth={lineWidth} left={c.TIMELINE_LEFT_TIME_PADDING} />
            <CircleAtEnd
                lineWidth={lineWidth}
                leftPadding={c.TIMELINE_LEFT_TIME_PADDING}
            />

            {hourMarkers.map((hour) => (
                <TimeBorder
                    key={`hour-${hour}`}
                    color={teamColor}
                    left={calculateHourMarkerPosition(
                        hour,
                        contestInfo.contestLengthMs,
                        config,
                    )}
                    isPvp={config.isPvp}
                />
            ))}

            {runsResults.map((problemResult, index) => (
                <Problem
                    key={`${problemResult.problemId}-${problemResult.time}-${index}`}
                    problemResult={problemResult}
                    contestInfo={contestInfo}
                    syncStartTime={syncStartTime}
                    config={config}
                />
            ))}
        </TimeLineContainer>
    );
}

export default TimeLine;
