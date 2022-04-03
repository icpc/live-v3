import React, { Fragment } from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ProblemCell, RankCell, TeamNameCell } from "../../atoms/ContestCells";
import { Cell } from "../../atoms/Cell";
import { StarIcon } from "../../atoms/Star";
import { VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";

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
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
`;

const TeamInfoWrapper = styled.div`
  display: flex;
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

export const TeamView = ({ widgetData }) => {
    return <TeamViewContainer>
        <TeamInfo teamId={48}/>
        <ScoreboardColumn teamId={48}/>
    </TeamViewContainer>;
};
export default TeamView;
