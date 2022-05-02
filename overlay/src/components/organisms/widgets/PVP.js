import React, { Fragment, useState } from "react";
import styled, { keyframes } from "styled-components";
import { useDispatch, useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ProblemCell, RankCell, TeamNameCell } from "../../atoms/ContestCells";
import { Cell } from "../../atoms/Cell";
import { StarIcon } from "../../atoms/Star";
import { TEAM_VIEW_APPEAR_TIME, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { pushLog } from "../../../redux/debug";
import { DateTime } from "luxon";
import _ from "lodash";

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

const PVPPerson = styled.div`
    display: grid;
    grid-template-rows: repeat(2, 1fr);
    grid-template-columns: repeat(2, 1fr);
    width: 100%;
    height: 100%;
`;

const TeamInfoWrapper = styled.div`
  display: flex;
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
    [TeamTaskStatus.untouched]: undefined,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const TeamTaskSymbol = Object.freeze({
    [TeamTaskStatus.solved]: "+",
    [TeamTaskStatus.failed]: "-",
    [TeamTaskStatus.untouched]: "",
    [TeamTaskStatus.unknown]: "?",
    [TeamTaskStatus.first]: "+",
});

const ScoreboardTaskCell = ({ status, attempts }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {TeamTaskSymbol[status]}
        {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
    </ScoreboardTaskCellWrap>;
};

const StatisticsProblemCell = styled(ProblemCell)`
    padding: 0 10px;
    width: 50px;
    box-sizing: border-box;
`;

const ScoreboardTimeCell = styled(ScoreboardCell)`
   flex-grow: 1;
    flex-shrink: 1;
    flex-basis: 100%;
    height: 100%;
    padding: 5px;
    padding-left: 10px;
    min-width: 40px;
`;

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

const ScoreboardColumnWrapper = styled.div.attrs(({ row }) => ({ style: { gridRow: row / row } }))`
    display: grid;
    grid-template-columns: repeat(2, auto);
    grid-auto-rows: 1fr;
    position: relative;
`;
const ScoreboardTeamInfoRow = styled.div`
    grid-column-start: 1;
    grid-column-end: 3;
`;
const TaskRow = styled.div`
    display: flex;
    width: 100%;
    grid-column-start: 2;
    grid-column-end: 3;
`;

const ScoreboardColumn = ({ teamId, row }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    console.log(scoreboardData);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardColumnWrapper row={row}>
        <ScoreboardTeamInfoRow>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRow>
        {_.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs").flatMap(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve, lastSubmitTimeMs, index }, i) =>
            getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) === TeamTaskStatus.untouched ? null : <TaskRow key={i}>
                <ScoreboardTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</ScoreboardTimeCell>
                <StatisticsProblemCell probData={tasks[index]}/>
                <ScoreboardTaskCell status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)} attempts={wrongAttempts + pendingAttempts}/>
            </TaskRow>
        )}
    </ScoreboardColumnWrapper>;
};

const TeamInfo = ({ teamId }) => {
    // console.log(useSelector((state) => state.contestInfo.info?.teamsId[teamId]));
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    console.log(teamData);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"}/>
        <TeamNameCell teamName={teamData?.shortName} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData?.totalScore}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>

    </TeamInfoWrapper>;
};

const TeamVideoWrapper =styled.video`
  position: absolute;
  top: 0;
  left: 0;
  display: block;
  height: 100%;
  object-fit: contain;
`;

const PVPTeamVideoWrapper =styled.div`
    position: absolute;
    display: flex;
    flex-direction: row;
    width: 100%;
    height: 100%; 
    overflow: hidden;
    justify-content: flex-end;
`;

const PVPContainer = styled.div`
  width: 100%;
  height: 100%;
  overflow: hidden;
  display: ${props => props.show ? "flex" : "none"};
  flex-direction: column;
  justify-content: start;
  align-items: flex-end;
  position: relative;
`;

const TeamVideoAnimationWrapper = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

const TeamVideo = ({ teamId, type, setIsLoaded }) => {
    const dispatch = useDispatch();

    const medias = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    if(!medias)
        return null;
    return <TeamVideoAnimationWrapper>
        <TeamVideoWrapper
            src={medias.medias[type]}
            onCanPlay={() => setIsLoaded(true)}
            onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
            autoPlay
            muted/>

    </TeamVideoAnimationWrapper>;
};

const Shit = styled.div`
  width: 100%;
  height: 100%;
  overflow: hidden;
` ;

const PVPTeam = ({ media, teamId, setIsLoaded }) => {
    var mediaOptions = [];

    for (const type in media) {
        mediaOptions.push(
            <div>
                {type !== undefined && <TeamVideo teamId={teamId} type={type} setIsLoaded={setIsLoaded}/>}
            </div>
        );
    }
    console.log(mediaOptions);
    return mediaOptions;
};


export const PVP = ({ widgetData: { settings }, transitionState }) => {
    const first = /*useState(settings.first)*/ 0;
    const second = /*useState(settings.second)*/ 1;
    const media = /*useSelector(settings.mediaTypes)*/ ["camera"];
    const [isLoaded, setIsLoaded] = useState(media === undefined);
    return <PVPPerson>
        <PVPContainer
            show={isLoaded}>
            <Shit>{settings.mediaType !== undefined &&
                <TeamVideo teamId={first} type={media[0]} setIsLoaded={setIsLoaded}/>
            }</Shit>
            <Shit>{settings.mediaType !== undefined &&
                <TeamVideo teamId={first} type={media[0]} setIsLoaded={setIsLoaded}/>}
            </Shit>
            <ScoreboardColumn teamId={first} row="1"/>
            <Shit>{settings.mediaType !== undefined &&
                <TeamVideo teamId={first} type={media[0]} setIsLoaded={setIsLoaded}/>
            }</Shit>
            <Shit>{settings.mediaType !== undefined &&
                <TeamVideo teamId={first} type={media[0]} setIsLoaded={setIsLoaded}/>}
            </Shit>
            <ScoreboardColumn teamId={second} row="2"/>
        </PVPContainer>
    </PVPPerson>;
};

export default PVP;
