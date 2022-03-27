import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { SCOREBOARD_ROW_TRANSITION_TIME, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, RankCell, TeamNameCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
}

const NUM = 30;
const NUMWIDTH = 80;
const NAMEWIDTH = 300;
const STATWIDTH = 80;

const ScoreboardWrap = styled.div`
  height: 100%;
  width: 100%;
  opacity: 0.8;
  border: none;
  border-collapse: collapse;
  table-layout: fixed;
`;

const ScoreboardRowContainer = styled.div`
  height: 100%;
  width: 100%;
  display: flex;
`;

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

const ScoreboardTaskCellWrap = styled(ScoreboardCell)`
  flex-grow: 1;
  flex-shrink: 1;
  flex-basis: 100%;
`;

const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});

const TeamTaskSymbol = Object.freeze({
    [TeamTaskStatus.solved]: "+",
    [TeamTaskStatus.failed]: "-",
    [TeamTaskStatus.untouched]: "",
    [TeamTaskStatus.unknown]: "?",
    [TeamTaskStatus.first]: "+",
});

const TeamTaskColor = Object.freeze({
    [TeamTaskStatus.solved]: VERDICT_OK,
    [TeamTaskStatus.failed]: VERDICT_NOK,
    [TeamTaskStatus.untouched]: undefined,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const ScoreboardTaskCell = ({ status, attempts }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {TeamTaskSymbol[status]}
        {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
    </ScoreboardTaskCellWrap>;
};

ScoreboardTaskCell.propTypes = {
    status: PropTypes.oneOf(Object.values(TeamTaskStatus)),
    attempts: PropTypes.number
};

const ScoreboardHeaderWrap = styled(ScoreboardRowContainer)`
  height: ${props => props.rowHeight}px;
`;

const ScoreboardHeaderTitle = styled(ScoreboardCell)`
  background: red;
  width: ${NUMWIDTH + NAMEWIDTH}px;
  font-size: 30px;
`;

const ScoreboardHeaderStatCell = styled(ScoreboardStatCell)`
  background: black;
  width: ${STATWIDTH}px;
  text-align: center;
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

const ScoreboardRow = ({ teamId }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    return <ScoreboardRowContainer>
        <RankCell rank={scoreboardData.rank} width={NUMWIDTH + "px"}/>
        <TeamNameCell teamName={teamData.shortName} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData.totalScore}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData.penalty}
        </ScoreboardStatCell>
        {scoreboardData.problemResults.map(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve }, i) =>
            <ScoreboardTaskCell key={i} status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                attempts={wrongAttempts + pendingAttempts}/>
        )}
    </ScoreboardRowContainer>;
};
ScoreboardRow.propTypes = {
    teamId: PropTypes.number.isRequired
};

const ScoreboardHeader = ({ problems, rowHeight }) => {
    return <ScoreboardHeaderWrap rowHeight={rowHeight}>
        <ScoreboardHeaderTitle>CURRENT STANDINGS</ScoreboardHeaderTitle>
        <ScoreboardHeaderStatCell>&#931;</ScoreboardHeaderStatCell>
        <ScoreboardHeaderStatCell>PEN</ScoreboardHeaderStatCell>
        {problems && problems.map((probData) =>
            <ProblemCell key={probData.name} probData={probData} canGrow={true} canShrink={true} basis={"100%"}/>
        )}
    </ScoreboardHeaderWrap>;
};

ScoreboardHeader.propTypes = {
    problems: PropTypes.arrayOf(PropTypes.object)
};

const PositionedScoreboardRowWrap = styled.div.attrs((props) => ({
    style: {
        top: props.pos + "px"
    }
}))`
  left: 0;
  right: 0;
  height: ${props => props.rowHeight}px;
  transition: top ${SCOREBOARD_ROW_TRANSITION_TIME}ms ease-out;
  position: absolute;
`;

const PositionedScoreboardRow = ({ zIndex, children, ...rest }) => {
    return <div style={{ zIndex: zIndex }}>
        <PositionedScoreboardRowWrap {...rest}>
            {children}
        </PositionedScoreboardRowWrap>
    </div>;
};

PositionedScoreboardRow.propTypes = {
    zIndex: PropTypes.number,
    children: PropTypes.node
};

const scrollTime = 1000;
const teamsOnPage = 23;
const maxPages = 1;
const SCOREBOARD_TYPE = SCOREBOARD_TYPES.normal;

export const Scoreboard = ({ widgetData }) => {
    let rows = useSelector((state) => state.scoreboard[SCOREBOARD_TYPE].rows);
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const [offset, setOffset] = useState(0);
    const totalHeight = widgetData.location.sizeY;
    const rowHeight = (totalHeight / (teamsOnPage + 1));
    useEffect(() => {
        const id = setInterval(() => {
            setOffset((offset) => {
                let newStart = offset + teamsOnPage;
                return (newStart >= Math.min(rows.length, maxPages * teamsOnPage) ? 0 : newStart);
            });
        }, scrollTime);
        return () => clearInterval(id);
    }, [rows.length]);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    console.log(teams);
    return <ScoreboardWrap>
        <div>
            {teams.map(([ind, teamRowData]) =>
                <PositionedScoreboardRow key={teamRowData.teamId} pos={(ind - offset) * rowHeight + rowHeight}
                    rowHeight={rowHeight} zIndex={ind}>
                    <ScoreboardRow teamId={teamRowData.teamId}/>
                </PositionedScoreboardRow>
            )}
        </div>
        <ScoreboardHeader problems={contestInfo?.problems} rowHeight={rowHeight} key={"header"}/>
    </ScoreboardWrap>;
};

Scoreboard.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard;
