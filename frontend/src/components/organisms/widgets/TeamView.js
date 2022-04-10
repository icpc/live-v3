import React, { Fragment, useState } from "react";
import styled, { keyframes } from "styled-components";
import { useDispatch, useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ProblemCell, RankCell, TeamNameCell } from "../../atoms/ContestCells";
import { Cell } from "../../atoms/Cell";
import { StarIcon } from "../../atoms/Star";
import { TEAM_VIEW_APPEAR_TIME, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
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

const TeamViewContainer = styled.div`
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  flex-direction: column;
  justify-content: start;
  align-items: flex-end;
  position: relative;
  animation: ${props => props.animation} ${TEAM_VIEW_APPEAR_TIME}ms ${props => props.animationStyle};
`;

const TeamInfoWrapper = styled.div`
  display: flex;
  position: relative;
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

const ScoreboardColumnWrapper = styled.div`
  display: grid;
  grid-template-columns: auto 1fr;  
  position: relative;
`;

const ScoreboardColumn = ({ teamId }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardColumnWrapper>
        {scoreboardData?.problemResults.map(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve }, index) =>
            <Fragment key={index}>
                {getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) !== TeamTaskStatus.untouched &&
                    <StatisticsProblemCell probData={tasks[index]}/>}

                {getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) !== TeamTaskStatus.untouched &&
                    <ScoreboardTaskCell status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)} attempts={wrongAttempts + pendingAttempts}/>
                }

            </Fragment>
        )}
    </ScoreboardColumnWrapper>;
};

const TeamInfo = ({ teamId }) => {
    console.log(useSelector((state) => state.contestInfo.info?.teamsId[teamId]));
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
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
    width: 100%;
    top: 0;
`;

const TeamVideoAnimationWrapper = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: start;
  align-items: center;
`;

const TeamVideo = ({ teamId, type, setIsLoaded }) => {
    const dispatch = useDispatch();

    const medias = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    if(!medias)
        return null;
    return <TeamVideoAnimationWrapper>
        <TeamVideoWrapper
            src={"https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"}
            onCanPlay={() => setIsLoaded(true)}
            onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
            autoPlay
            muted/>
    </TeamVideoAnimationWrapper>;
};

export const TeamView = ({ widgetData: { settings }, transitionState }) => {
    console.log(settings);
    const [isLoaded, setIsLoaded] = useState(false);

    return <TeamViewContainer
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        <TeamVideo teamId={settings.teamId} type={settings.mediaType} setIsLoaded={setIsLoaded}/>
        <TeamInfo teamId={settings.teamId}/>
        <ScoreboardColumn teamId={settings.teamId}/>
    </TeamViewContainer>;
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = TEAM_VIEW_APPEAR_TIME;

export default TeamView;
