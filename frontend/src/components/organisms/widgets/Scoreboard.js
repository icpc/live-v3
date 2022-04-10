import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    SCOREBOARD_HEADER_BG_COLOR,
    SCOREBOARD_HEADER_TITLE_BG_COLOR,
    SCOREBOARD_HEADER_TITLE_FONT_SIZE,
    SCOREBOARD_NAME_WIDTH,
    SCOREBOARD_OPACITY,
    SCOREBOARD_RANK_WIDTH,
    SCOREBOARD_ROW_TRANSITION_TIME,
    SCOREBOARD_SCROLL_INTERVAL,
    SCOREBOARD_SUM_PEN_WIDTH,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../../config";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, RankCell, TeamNameCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";


const ScoreboardWrap = styled.div`
  height: 100%;
  width: 100%;
  opacity: ${SCOREBOARD_OPACITY};
  border: none;
  border-collapse: collapse;
  table-layout: fixed;
  display: flex;
  flex-direction: column;
`;

const ScoreboardRowContainer = styled.div`
  height: 100%;
  width: 100%;
  display: flex;
  overflow: hidden;
  //box-sizing: border-box;
`;

const ScoreboardCell = styled(Cell)`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  position: relative;
`;

const ScoreboardStatCell = styled(ScoreboardCell)`
  width: ${props => props.width};
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
  background: ${SCOREBOARD_HEADER_TITLE_BG_COLOR};
  width: calc(${SCOREBOARD_RANK_WIDTH} + ${SCOREBOARD_NAME_WIDTH});
  font-size: ${SCOREBOARD_HEADER_TITLE_FONT_SIZE};
`;

const ScoreboardHeaderStatCell = styled(ScoreboardStatCell)`
  background: ${SCOREBOARD_HEADER_BG_COLOR};
  width: ${SCOREBOARD_SUM_PEN_WIDTH};
  text-align: center;
`;

const ScoreboardHeaderProblemCell = styled(ProblemCell)`
  position: relative;
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

export const ScoreboardRow = ({ teamId, hideTasks, rankWidth, nameWidth, sumPenWidth, nameGrows, optimismLevel }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    return <ScoreboardRowContainer>
        <RankCell rank={scoreboardData.rank} width={rankWidth ?? SCOREBOARD_RANK_WIDTH}/>
        <TeamNameCell teamName={teamData.shortName} width={nameGrows ? undefined : (nameWidth ?? SCOREBOARD_NAME_WIDTH)} canGrow={nameGrows ?? false} canShrink={nameGrows?? false}/>
        <ScoreboardStatCell width={sumPenWidth ?? SCOREBOARD_SUM_PEN_WIDTH}>
            {scoreboardData.totalScore}
        </ScoreboardStatCell>
        <ScoreboardStatCell width={sumPenWidth ?? SCOREBOARD_SUM_PEN_WIDTH}>
            {scoreboardData.penalty}
        </ScoreboardStatCell>
        {!hideTasks && scoreboardData.problemResults.map(({ wrongAttempts, pendingAttempts, isSolved, isFirstToSolve }, i) =>
            <ScoreboardTaskCell key={i} status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                attempts={wrongAttempts + pendingAttempts}/>
        )}
    </ScoreboardRowContainer>;
};
ScoreboardRow.propTypes = {
    teamId: PropTypes.number.isRequired,
    hideTasks: PropTypes.bool
};

const ScoreboardHeader = ({ problems, rowHeight }) => {
    return <ScoreboardHeaderWrap rowHeight={rowHeight}>
        <ScoreboardHeaderTitle>CURRENT STANDINGS</ScoreboardHeaderTitle>
        <ScoreboardHeaderStatCell>&#931;</ScoreboardHeaderStatCell>
        <ScoreboardHeaderStatCell>PEN</ScoreboardHeaderStatCell>
        {problems && problems.map((probData) =>
            <ScoreboardHeaderProblemCell key={probData.name} probData={probData} canGrow={true} canShrink={true}
                basis={"100%"}/>
        )}
    </ScoreboardHeaderWrap>;
};

ScoreboardHeader.propTypes = {
    problems: PropTypes.arrayOf(PropTypes.object)
};

const ScoreboardRowWrap = styled.div.attrs((props) => ({
    style: {
        top: props.pos + "px"
    }
}))`
  left: 0;
  right: 0;
  height: ${props => props.rowHeight + 2}px; // FIXME lol
  transition: top ${SCOREBOARD_ROW_TRANSITION_TIME}ms ease-out;
  position: absolute;
`;
/**
 * Aligned vertically with zIndex
 * @type {StyledComponent<"div", AnyIfEmpty<DefaultTheme>, function({zIndex: *}): {style: {zIndex: *}}, keyof function({zIndex: *}): {style: {zIndex: *}}>}
 */
const PositionedScoreboardRowWrap = styled.div.attrs(({ zIndex }) => ({
    style: {
        zIndex: zIndex
    }
}
))`
  position: relative;
`;

const PositionedScoreboardRow = ({ zIndex, children, ...rest }) => {
    return <PositionedScoreboardRowWrap zIndex={zIndex}>
        <ScoreboardRowWrap {...rest}>
            {children}
        </ScoreboardRowWrap>
    </PositionedScoreboardRowWrap>;
};

PositionedScoreboardRow.propTypes = {
    zIndex: PropTypes.number,
    children: PropTypes.node
};

export const Scoreboard = ({ widgetData: { settings, location } }) => {
    const optimismLevel = settings.optimismLevel;
    const teamsOnPage = settings.teamsOnPage;
    let rows = useSelector((state) => state.scoreboard[optimismLevel].rows);
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const startPageRow = (settings.startFromPage - 1) * teamsOnPage;
    const [row, setRow] = useState(startPageRow);
    const totalHeight = location.sizeY;
    const rowHeight = (totalHeight / (teamsOnPage + 1));
    useEffect(() => {
        const id = setInterval(() => {
            setRow((offset) => {
                let newStart = offset + teamsOnPage;
                if (newStart >= Math.min(rows.length, ((settings.numPages || Infinity) + startPageRow) * teamsOnPage)) {
                    if(settings.isInfinite) {
                        return startPageRow;
                    } else {
                        return offset;
                    }
                } else {
                    return newStart;
                }
            });
        }, SCOREBOARD_SCROLL_INTERVAL);
        return () => clearInterval(id);
    }, [rows.length]);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    return <ScoreboardWrap>
        <ScoreboardHeader problems={contestInfo?.problems} rowHeight={rowHeight} key={"header"}/>
        <div style={{ overflow: "hidden", height: "100%" }}>
            {teams.map(([ind, teamRowData]) =>
                <PositionedScoreboardRow key={teamRowData.teamId} pos={(ind - row) * rowHeight}
                    rowHeight={rowHeight} zIndex={rows.length - ind}>
                    <ScoreboardRow teamId={teamRowData.teamId} optimismLevel={optimismLevel}/>
                </PositionedScoreboardRow>
            )}
        </div>
    </ScoreboardWrap>;
};

Scoreboard.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard;
