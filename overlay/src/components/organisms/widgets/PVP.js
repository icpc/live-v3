import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import { useDispatch, useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { Cell } from "../../atoms/Cell";
import { StarIcon } from "../../atoms/Star";
import {
    PVP_OPACITY,
    STATISTICS_BG_COLOR,
    PVP_APPEAR_TIME,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../../config";
import { pushLog } from "../../../redux/debug";

const slideIn = keyframes`
  from {
    opacity: 0.1;
  }
  to {
     opacity: 1;
  }
`;

const slideOut = keyframes`
  from {
     opacity: 1;
  }
  to {
      opacity: 0.1;
  }
`;

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

const PVPPerson = styled.div.attrs(({ scale }) => ({ style: { gridTemplateColumns: scale } }))`
  
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "grid" : "none"};
  grid-template-rows: 50% 50%;
  grid-auto-flow: row;
  align-items: end;
  animation: ${props => props.animation} ${PVP_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
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

const StatisticsProblemCellWithColor = ({ probData, status }) => {
    console.log(probData);
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
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <ScoreboardTeamInfoRowFirst>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowFirst>
        <TaskRowWrapperFirst>
            {scoreboardData?.problemResults.flatMap(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve, lastSubmitTimeMs, index }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]} status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}/>
                </TaskRow>
            )}
        </TaskRowWrapperFirst>
    </ScoreboardRowAllWrapper>;
};

const ScoreboardRowAllTaskSecond = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    //console.log(scoreboardData);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <TaskRowWrapperSecond>
            {scoreboardData?.problemResults.flatMap(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve, lastSubmitTimeMs, index }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]} status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)} attempts={wrongAttempts + pendingAttempts}/>
                </TaskRow>
            )}
        </TaskRowWrapperSecond>
        <ScoreboardTeamInfoRowSecond>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowSecond>
    </ScoreboardRowAllWrapper>;
};

const TeamInfo = ({ teamId }) => {
    // console.log(useSelector((state) => state.contestInfo.info?.teamsId[teamId]));
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    console.log(teamData);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData?.totalScore}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>

    </TeamInfoWrapper>;
};

const TeamVideoWrapper =styled.video.attrs(({ position }) => ({ style: { objectPosition: position } }))`
  object-fit: contain;
  width: 100%;
  height: 100%;
`;

const TeamVideoAnimationWrapper = styled.section`
  height: 100%;
  position: relative;
`;

const TeamVideoContainer = styled.section`
  position: absolute;
  height: 100%;
  bottom: ${props => props.bottom};
  top: ${props => props.top};
`;

const TeamVideo = ({ teamId, type, setIsLoaded, position, bottom, top }) => {
    const dispatch = useDispatch();
    console.log(bottom);
    const medias = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    if(!medias)
        return null;
    return<TeamVideoAnimationWrapper>
        <TeamVideoContainer bottom={bottom} top={top}>
            <TeamVideoWrapper
                position={position}
                src={medias.medias[type]}
                onCanPlay={() => setIsLoaded(true)}
                onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
                autoPlay
                muted/>
        </TeamVideoContainer>
    </TeamVideoAnimationWrapper>;
};

const ScoreboardWrapper = styled.div.attrs( ({ align }) => ({ style: { justifyContent: align } }) )`
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
  opacity: ${PVP_OPACITY};
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-rows: 50% 50%;
  grid-auto-flow: column;
  justify-items: start;
` ;

const PVPWrapper = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
` ;



export const PVP = ({ widgetData: { settings }, transitionState }) => {
    const teamIds = settings.teamId;
    const media = settings?.mediaTypes;
    const [isLoaded, setIsLoaded] = useState(media === undefined);
    let scale = [100];
    for (let i = 1; i < media.length; i++) {
        scale.push(scale[i - 1] * 0.4);
        scale[i - 1] *= 0.6;
    }
    scale.reverse();
    return <PVPWrapper>
        <PVPPerson
            show={isLoaded}
            scale={scale.join("% ") + "%"}
            animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
            animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}>
            <TeamVideo teamId={teamIds[0]} type={media[0]} setIsLoaded={setIsLoaded} bottom={"80px"} top={"auto"} position={"100% bottom"}/>
            <TeamVideo teamId={teamIds[0]} type={media[1]} setIsLoaded={setIsLoaded} position={"right bottom"}/>
            <TeamVideo teamId={teamIds[1]} type={media[0]} setIsLoaded={setIsLoaded} top={"80px"} position={"100% top"}/>
            <TeamVideo teamId={teamIds[1]} type={media[1]} setIsLoaded={setIsLoaded} position={"right top"}/>
        </PVPPerson>
        <PVPInfo>
            <ScoreboardWrapper align={"end"}>
                <ScoreboardRowAllTaskFirst teamId={teamIds[0]}/>
            </ScoreboardWrapper>
            <ScoreboardWrapper align={"start"}>
                <ScoreboardRowAllTaskSecond teamId={teamIds[1]}/>
            </ScoreboardWrapper>
        </PVPInfo>
    </PVPWrapper>;
};
PVP.ignoreAnimation = true;
PVP.overrideTimeout = PVP_APPEAR_TIME;
export default PVP;
