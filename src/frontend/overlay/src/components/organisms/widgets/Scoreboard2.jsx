import _ from "lodash";
import PropTypes from "prop-types";
import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    QUEUE_PER_COLUMNS_PADDING2,
    SCOREBOARD_NAME_WIDTH2,
    SCOREBOARD_ROW_TRANSITION_TIME,
    SCOREBOARD_SCROLL_INTERVAL,
    SCOREBOARD_BACKGROUND_COLOR,
    SCOREBOARD_CAPTION,
    SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR,
    SCOREBOARD_TABLE_HEADER_DIVIDER_COLOR,
} from "../../../config";
import { formatScore } from "../../atoms/ContestCells";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { extractScoreboardRows, useScroller } from "./Scoreboard";
import { ContestantRow2 } from "../../atoms/ContestantRow2";
import { TaskResultLabel2, RankLabel } from "../../atoms/ContestLabels2";
import { FlexedBox2, ShrinkingBox2 } from "../../atoms/Box2";


const ScoreboardWrap = styled.div`
  color: #FFF;
  height: 100%;
  width: 100%;
  display: flex;
  gap: 14px;
  padding: 7px 16px 0 16px;
  box-sizing: border-box;
  flex-direction: column;
  background-color: ${SCOREBOARD_BACKGROUND_COLOR};
  border-radius: 16px;
  overflow: hidden;
`;

const ScoreboardHeader = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  font-size: 32px;
  font-style: normal;
  font-weight: 700;
  line-height: 44px; /* 137.5% */
`;

const ScoreboardTitle = styled.div`
  flex: 1 0 0;
`;

const ScoreboardCaption = styled.div`
`;

const ScoreboardContent = styled.div`
  flex: 1 0 0;
  display: flex;
  flex-direction: column;
`;


export const nameTable = {
    normal: "Current",
    optimistic: "Optimistic",
    pessimistic: "Pessimistic",
};

export const ScoreboardRow2 = ({ teamId, hideTasks, optimismLevel }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const contestData = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    return <ContestantRow2 medal={scoreboardData?.medalType}>
        <RankLabel rank={scoreboardData?.rank} width={"50px"}/>
        <ShrinkingBox2 text={teamData?.shortName ?? "??"} Wrapper={FlexedBox2}
            marginLeft={QUEUE_PER_COLUMNS_PADDING2} marginRight={QUEUE_PER_COLUMNS_PADDING2}
            width={SCOREBOARD_NAME_WIDTH2}/>
        <ShrinkingBox2 align={"center"} Wrapper={FlexedBox2}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
            flexGrow={1} flexShrink={1} flexBasis={0}/>
        {contestData?.resultType === "ICPC" && <ShrinkingBox2 align={"center"} Wrapper={FlexedBox2}
            text={scoreboardData?.penalty} flexGrow={1} flexShrink={1} flexBasis={0}/>}
        {!hideTasks && scoreboardData?.problemResults.map((result, i) =>
            <FlexedBox2 flexGrow={1} flexShrink={1} flexBasis={0} align={"center"} key={i}>
                <TaskResultLabel2 problemResult={result}
                    minScore={contestData?.problems[i]?.minScore} maxScore={contestData?.problems[i]?.maxScore}/>
            </FlexedBox2>)}
    </ContestantRow2>;
};
ScoreboardRow2.propTypes = {
    teamId: PropTypes.number.isRequired,
    hideTasks: PropTypes.bool
};

const PositionedScoreboardRow = styled.div.attrs(({ zIndex, pos }) => ({
    style: {
        zIndex: zIndex,
        top: pos + "px",
    }
}))`
  height: ${props => props.rowHeight + 2}px; /* FIXME lol */
  transition: top ${SCOREBOARD_ROW_TRANSITION_TIME}ms ease-out;
  left: 0;
  right: 0;
  width: 100%;
  position: absolute;
`;

const ScoreboardRowsWrap = styled.div`
  position: relative;
  flex: 1 0 0;
  overflow: hidden;
`;

export const ScoreboardRows = ({ settings }) => {
    const rows = extractScoreboardRows(
        useSelector((state) => state.scoreboard[settings.optimismLevel]),
        settings.group);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    const rowHeight = 44;
    const scrollPos = useScroller(rows.length, settings.teamsOnPage, SCOREBOARD_SCROLL_INTERVAL, settings.startFromRow - 1, settings.numRows);
    return <ScoreboardRowsWrap>
        {teams.map(([index, teamData]) =>
            <PositionedScoreboardRow key={teamData.teamId} zIndex={index} pos={index * rowHeight - scrollPos * rowHeight}>
                <ScoreboardRow2 key={teamData.teamId} teamId={teamData.teamId} optimismLevel={settings.optimismLevel}/>
            </PositionedScoreboardRow>
        )}
    </ScoreboardRowsWrap>
}

const ScoreboardTableHeaderWrap = styled.div`
  background-color: ${SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR};
  padding-left: 10px;
  gap: 8px;
  box-sizing: border-box;
  display: flex;
  border-radius: 16px 16px 0 0;
  overflow: hidden;

  font-size: 21px;
  font-style: normal;
  font-weight: 700;
  line-height: 44px;
  
`;

const ScoreboardTableHeaderCell = styled.div`
  flex-shrink: 0;
  text-align: center;
`;

const ScoreboardTableHeaderPlace = styled(ScoreboardTableHeaderCell)`
  width: 55px;
`

const ScoreboardTableHeaderName = styled(ScoreboardTableHeaderCell)`
  width: 288px;
`

const ScoreboardTableHeaderPoints = styled(ScoreboardTableHeaderCell)`
  width: 65px;
`

const ScoreboardTableHeaderPenalty = styled(ScoreboardTableHeaderCell)`
  width: 76px;
`

const TableDivider = styled.div`
  width: 3px;
  flex-shrink: 0;
  align-self: stretch;
  background-color: ${SCOREBOARD_TABLE_HEADER_DIVIDER_COLOR};
`

const ScoreboardTableHeaderProblems = styled.div`
    display: flex;
    align-items: center;
    flex: 1 0 0;
    align-self: stretch;
`

const ScoreboardProblemLabel = styled(ProblemLabel)`
  flex: 1 0 0;
`

const ScoreboardTableHeader = () => {
    const problems = useSelector((state) => state.contestInfo.info?.problems);
    return <ScoreboardTableHeaderWrap>
        <ScoreboardTableHeaderPlace>Place</ScoreboardTableHeaderPlace>
        <TableDivider/>
        <ScoreboardTableHeaderName>Team name</ScoreboardTableHeaderName>
        <TableDivider/>
        <ScoreboardTableHeaderPoints>Points</ScoreboardTableHeaderPoints>
        <TableDivider/>
        <ScoreboardTableHeaderPenalty>Penalty</ScoreboardTableHeaderPenalty>
        <ScoreboardTableHeaderProblems>
            {problems && problems.map((probData) => {
                    return <React.Fragment key={probData.name}>
                        <TableDivider/>
                        <ScoreboardProblemLabel letter={probData.letter} problemColor={probData.color}/>
                    </React.Fragment>;
                }
            )}
        </ScoreboardTableHeaderProblems>
    </ScoreboardTableHeaderWrap>;
}

ScoreboardTableHeader.propTypes = { name: PropTypes.any };
export const Scoreboard2 = ({ widgetData: { settings } }) => {
    return <ScoreboardWrap>
        <ScoreboardHeader>
            <ScoreboardTitle>
                {nameTable[settings.optimismLevel] ?? "??"} standings
            </ScoreboardTitle>
            <ScoreboardCaption>
                {SCOREBOARD_CAPTION}
            </ScoreboardCaption>
        </ScoreboardHeader>
        <ScoreboardContent>
            <ScoreboardTableHeader/>
            <ScoreboardRows settings={settings}/>
        </ScoreboardContent>
    </ScoreboardWrap>;
};

Scoreboard2.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard2;
