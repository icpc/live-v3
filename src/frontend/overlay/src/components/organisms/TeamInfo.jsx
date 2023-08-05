import _ from "lodash";
import { DateTime } from "luxon";
import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_VERDICT_WIDTH,
    TEAMVIEW_SMALL_FACTOR,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../config";
import { SCOREBOARD_TYPES } from "../../consts";
import { Cell } from "../atoms/Cell";
import { formatScore, ProblemCell, RankCell, TextShrinkingCell } from "../atoms/ContestCells";
import { StarIcon } from "../atoms/Star";
// import { ScoreboardIOITaskCell } from "./widgets/Scoreboard";

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
  padding: 5px 5px 5px 10px;
  min-width: 40px;
`;

export function getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
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

// opacity: ${TEAM_VIEW_OPACITY};
const ScoreboardColumnWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(2, auto);
  grid-auto-rows: 1fr;
  position: absolute;
  transform-origin: top right;
  transform: ${props => props.isSmall ? `scale(${TEAMVIEW_SMALL_FACTOR})` : ""};
  white-space: nowrap;
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
export const ScoreboardColumn = ({ teamId, isSmall }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);
    if (contestData === undefined) {
        return null;
    }

    if (!contestData) {
        return null;
    } else if (contestData.resultType === "ICPC") {
        return <ScoreboardColumnWrapper isSmall={isSmall}>
            <ScoreboardTeamInfoRow>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRow>
            {_.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs").flatMap(({
                    wrongAttempts,
                    pendingAttempts,
                    isSolved,
                    isFirstToSolve,
                    lastSubmitTimeMs,
                    index
                }, i) =>
                    getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) === TeamTaskStatus.untouched ? null :
                        <TaskRow key={i}>
                            <ScoreboardTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</ScoreboardTimeCell>
                            <StatisticsProblemCell probData={tasks[index]}/>
                            <ScoreboardTaskCell status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                                                attempts={wrongAttempts + pendingAttempts}/>
                        </TaskRow>
            )}
        </ScoreboardColumnWrapper>;
    } else {
        return <ScoreboardColumnWrapper isSmall={isSmall}>
            <ScoreboardTeamInfoRow>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRow>
            {_.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs").flatMap(({
                    score,
                    lastSubmitTimeMs,
                    index
                }, i) =>
                    (score === undefined || lastSubmitTimeMs === undefined) ? null :
                        <TaskRow key={i}>
                            <ScoreboardTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</ScoreboardTimeCell>
                            <StatisticsProblemCell probData={tasks[index]}/>
                            {/*<ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={score}*/}
                            {/*                       minScore={contestData?.problems[index]?.minScore}*/}
                            {/*                       maxScore={contestData?.problems[index]?.maxScore}/>*/}
                        </TaskRow>
            )}
        </ScoreboardColumnWrapper>;
    }
};
export const TeamInfo = ({ teamId }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName ?? ""} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? null : formatScore(scoreboardData?.totalScore, 1)}
        </ScoreboardStatCell>
        {contestInfo?.resultType !== "IOI" &&
            <ScoreboardStatCell>
                {scoreboardData?.penalty}
            </ScoreboardStatCell>}

    </TeamInfoWrapper>;
};
