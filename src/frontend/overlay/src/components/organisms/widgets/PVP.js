import React, { useEffect, useLayoutEffect } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_VERDICT_WIDTH,
    PVP_APPEAR_TIME,
    STATISTICS_BG_COLOR,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { Cell } from "../../atoms/Cell";
import { formatScore, RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";
import { ScoreboardIOITaskCell } from "./Scoreboard";
import { TeamWebRTCProxyVideoWrapper, TeamWebRTCGrabberVideoWrapper } from "../holder/TeamViewHolder";

const NUMWIDTH = 80;
const NAMEWIDTH = 300;
const STATWIDTH = 80;
const ScoreboardCell = styled(Cell)`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  position: relative;
`;
const ScoreboardStatCell = styled(ScoreboardCell)`
  width: ${STATWIDTH}px;
`;


const TeamInfoWrapper = styled.div`
  display: flex;
  grid-row: 1;
  position: relative;
  height: 100%;
`;
const ScoreboardTaskCellWrap = styled(ScoreboardCell)`
  flex-grow: 1;
  flex-shrink: 1;
  flex-basis: 100%;
  height: 100%;
  padding: 5px;
  min-width: 40px;
`;
const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});
const TeamTaskColor = Object.freeze({
    [TeamTaskStatus.solved]: VERDICT_OK,
    [TeamTaskStatus.failed]: VERDICT_NOK,
    [TeamTaskStatus.untouched]: STATISTICS_BG_COLOR,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const StatisticsProblemCellWithColorICPC = ({ probData, status }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {probData?.letter ?? "??"}
    </ScoreboardTaskCellWrap>;
};
function getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
    if (isFirstToSolve) {
        return TeamTaskStatus.first;
    } else if (isSolved) {
        return TeamTaskStatus.solved;
    } else if (pendingAttempts > 0) {
        return TeamTaskStatus.unknown;
    } else if (wrongAttempts > 0) {
        return TeamTaskStatus.failed;
    } else {
        return TeamTaskStatus.untouched;
    }
}
const ScoreboardRowAllWrapper = styled.div`
  display: grid;
  justify-content: start;
  grid-template-rows: 41.5px 41.5px;
  position: relative;
`;
const TaskRowWrapperFirst = styled.div`
  grid-row: 2 / 3;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;
  grid-auto-flow: column;
  position: relative;
`;
const ScoreboardTeamInfoRowFirst = styled.div`
  grid-row: 1 / 2;
`;
const TaskRowWrapperSecond = styled.div`
  grid-row: 1 / 2;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;
  grid-auto-flow: column;
  position: relative;
`;
const ScoreboardTeamInfoRowSecond = styled.div`
  grid-row: 2 / 3;
`;
const TaskRow = styled.div`
  display: flex;
  flex-direction: column;
`;

const ScoreboardRowAllTaskFirst = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    //console.log(scoreboardData);
    const contestData = useSelector((state) => state.contestInfo.info);

    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    if (contestData === undefined) {
        return null;
    }
    if (contestData.resultType === "icpc") {
        return <ScoreboardRowAllWrapper>
            <ScoreboardTeamInfoRowFirst>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRowFirst>
            <TaskRowWrapperFirst>
                {scoreboardData?.problemResults.flatMap(({
                    wrongAttempts,
                    pendingAttempts,
                    isSolved,
                    isFirstToSolve,
                    index
                }, i) =>
                    <TaskRow key={i}>
                        <StatisticsProblemCellWithColorICPC probData={tasks[index]}
                            status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}/>
                    </TaskRow>
                )}
            </TaskRowWrapperFirst>
        </ScoreboardRowAllWrapper>;
    } else {
        return <ScoreboardRowAllWrapper>
            <ScoreboardTeamInfoRowFirst>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRowFirst>
            <TaskRowWrapperFirst>
                {scoreboardData?.problemResults?.flatMap(({ score, index }, i) =>
                    <TaskRow key={i}>
                        {tasks !== undefined && (tasks[index].letter !== "*" || score !== undefined) &&
                            <ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={score}  minScore={contestData?.problems[index]?.minScore} maxScore={contestData?.problems[index]?.maxScore}/>
                        }
                        {tasks !== undefined && tasks[index].letter === "*" && score === undefined &&
                            <ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={"*"}/>
                        }
                    </TaskRow>
                )}
            </TaskRowWrapperFirst>
        </ScoreboardRowAllWrapper>;
    }
};
const ScoreboardRowAllTaskSecond = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    //console.log(scoreboardData);
    const contestData = useSelector((state) => state.contestInfo.info);

    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    if(contestData === undefined) {
        return null;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    if (contestData.resultType === "icpc") {
        return <ScoreboardRowAllWrapper>
            <TaskRowWrapperSecond>
                {scoreboardData?.problemResults.flatMap(({
                    wrongAttempts,
                    pendingAttempts,
                    isSolved,
                    isFirstToSolve,
                    index
                }, i) =>
                    <TaskRow key={i}>
                        <StatisticsProblemCellWithColorICPC probData={tasks[index]}
                            status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                            attempts={wrongAttempts + pendingAttempts}/>
                    </TaskRow>
                )}
            </TaskRowWrapperSecond>
            <ScoreboardTeamInfoRowSecond>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRowSecond>
        </ScoreboardRowAllWrapper>;
    } else {
        return <ScoreboardRowAllWrapper>
            <TaskRowWrapperSecond>
                {scoreboardData?.problemResults.flatMap(({ score, index }, i) =>
                    <TaskRow key={i}>
                        {(tasks[index].letter !== "*" || score !== undefined) &&
                            <ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={score}  minScore={contestData?.problems[index]?.minScore} maxScore={contestData?.problems[index]?.maxScore}/>
                        }
                        {tasks[index].letter === "*" && score === undefined &&
                            <ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={"*"}/>
                        }
                    </TaskRow>
                )}
            </TaskRowWrapperSecond>
            <ScoreboardTeamInfoRowSecond>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRowSecond>
        </ScoreboardRowAllWrapper>;
    }
};

const TeamInfo = ({ teamId }) => {
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    const contestData = useSelector((state) => state.contestInfo.info);
    if(contestData === undefined) {
        return null;
    }
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName ?? ""} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? null : formatScore(scoreboardData?.totalScore, 1)}
        </ScoreboardStatCell>
        {contestData.resultType !== "ioi" &&
            <ScoreboardStatCell>
                {scoreboardData?.penalty}
            </ScoreboardStatCell>}

    </TeamInfoWrapper>;
};


const ScoreboardWrapper = styled.div.attrs(({ align }) => ({ style: { justifyContent: align } }))`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: start;
  position: relative;
  flex-direction: column;
  animation: ${props => props.animation} ${PVP_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;

const PVPInfo = styled.div`
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-items: start;
`;


const TeamImageWrapper = styled.img`
  height: 100%;
`;

const PVPVideoWrapper = styled.video`
  height: 100%;
`;


const TeamVideoAnimationWrapper = styled.div`
position: absolute;
right: 0;
object-fit: contain;
height: 100%;
display: flex;
justify-content: start;
align-items: center;
`;


const teamViewComponentRender = {
    Photo: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </TeamVideoAnimationWrapper>;
    },
    Object: ({ onLoadStatus, url }) => {
        onLoadStatus(true);
        return <TeamVideoAnimationWrapper>
            <object data={url} type="image/svg+xml">
            </object>
        </TeamVideoAnimationWrapper>;
    },
    Video: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <PVPVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, url, audioUrl }) => {
        return <TeamVideoAnimationWrapper>
            {audioUrl && <audio src={audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper Wrapper={PVPVideoWrapper} url={url} setIsLoaded={onLoadStatus}/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <TeamVideoAnimationWrapper>
            <TeamWebRTCGrabberVideoWrapper Wrapper={PVPVideoWrapper} {...props}/>
        </TeamVideoAnimationWrapper>;
    },
};

const TeamViewHolder = ({ onLoadStatus, media }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} {...media}/>;
};


const TeamViewPInPWrapper = styled.div`
position: absolute;

width: ${({ sizeX }) => `${sizeX * 0.361}px`};
height: ${({ sizeX }) => `${sizeX * 0.52 * 0.4}px`};
left: 0;
bottom: ${({ bottom }) => `${bottom}`};
top: ${({ top }) => `${top}`};
`;

export const PVP = ({ mediaContent, settings, setLoadedComponents, location }) => {

    if (settings.position === "PVP_TOP") {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamViewPInPWrapper key={index} bottom={"80px"} top={"auto"} sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo key={index}>
                    <ScoreboardWrapper align={"end"}>
                        <ScoreboardRowAllTaskFirst teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    } else {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamViewPInPWrapper key={index} top={"80px"} bottom={"auto"} sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo key={index}>
                    <ScoreboardWrapper align={"start"}>
                        <ScoreboardRowAllTaskSecond teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    }
};
PVP.ignoreAnimation = true;
PVP.overrideTimeout = PVP_APPEAR_TIME;
export default PVP;
