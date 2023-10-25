import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
// import { extractScoreboardRows, useScroller } from "./Scoreboard";
import { TaskResultLabel, RankLabel } from "../../atoms/ContestLabels";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";

import {formatScore, useFormatPenalty, useNeedPenalty} from "../../../services/displayUtils";


const ScoreboardWrap = styled.div`
  color: ${c.SCOREBOARD_TEXT_COLOR};
  height: 100%;
  width: 100%;
  display: flex;
  gap: 14px;
  padding: 7px 16px 0 16px;
  box-sizing: border-box;
  flex-direction: column;
  background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
  border-radius: ${c.SCOREBOARD_BORDER_RADIUS};
  overflow: hidden;
`;

const ScoreboardHeader = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  font-size: ${c.SCOREBOARD_CAPTION_FONT_SIZE};
  font-style: normal;
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  padding-top: 0.3em;
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
  gap: ${c.SCOREBOARD_BETWIN_HEADER_PADDING}px;
`;


export const nameTable = {
    normal: "Current",
    optimistic: "Optimistic",
    pessimistic: "Pessimistic",
};

const ScoreboardTableRowWrap = styled.div`
  gap: ${c.SCOREBOARD_BETWIN_HEADER_PADDING}px;
  box-sizing: border-box;
  background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
  display: grid;
  grid-template-columns:
          ${c.SCOREBOARD_CELL_PLACE_SIZE}
          ${c.SCOREBOARD_CELL_TEAMNAME_SIZE} 
          ${c.SCOREBOARD_CELL_POINTS_SIZE} 
          ${c.SCOREBOARD_CELL_PENALTY_SIZE} 
          repeat(${props => props.nProblems}, 1fr);
`;

const ScoreboardRowWrap = styled(ScoreboardTableRowWrap)`
  height: ${c.SCOREBOARD_ROW_HEIGHT}px;
  overflow: hidden;
  box-sizing: content-box;
  border-top: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
  border-bottom: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
  
  font-size: ${c.SCOREBOARD_ROW_FONT_SIZE};
  font-style: normal;
  font-weight: ${c.SCOREBOARD_TABLE_ROW_FONT_WEIGHT};

  align-items: center;
`;

const ScoreboardRowName = styled(ShrinkingBox)`
  //font-weight: 700;
  padding: 0 8px;
`;

const ScoreboardRankLabel = styled(RankLabel)`
  align-self: stretch;
  
  display: flex;
  align-items: center;
  justify-content: center;
`;
export const ScoreboardTaskResultLabel = styled(TaskResultLabel)`
  align-self: stretch;

  display: flex;
  align-items: center;
  justify-content: center;
`;

export const ScoreboardRow = ({ teamId, hideTasks, optimismLevel }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const contestData = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const needPenalty = useNeedPenalty();
    const formatPenalty = useFormatPenalty();
    return <ScoreboardRowWrap medal={scoreboardData?.medalType} nProblems={contestData?.problems?.length ?? 1}>
        <ScoreboardRankLabel rank={scoreboardData?.rank} medal={scoreboardData?.medalType}/>
        <ScoreboardRowName align={c.SCOREBOARD_CELL_TEAMNANE_ALIGN} text={teamData?.shortName ?? "??"}/>
        <ShrinkingBox align={c.SCOREBOARD_CELL_POINTS_ALIGN}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}/>
        {needPenalty && <ShrinkingBox align={c.SCOREBOARD_CELL_PENALTY_ALIGN} text={
            formatPenalty(scoreboardData?.penalty)
        } />}
        {!hideTasks && scoreboardData?.problemResults.map((result, i) =>
            <ScoreboardTaskResultLabel problemResult={result} key={i}
                minScore={contestData?.problems[i]?.minScore} maxScore={contestData?.problems[i]?.maxScore}/>
        )}
    </ScoreboardRowWrap>;
};
ScoreboardRow.propTypes = {
    teamId: PropTypes.number.isRequired,
    hideTasks: PropTypes.bool
};

const PositionedScoreboardRow = styled.div.attrs(({ zIndex, pos }) => ({
    style: {
        zIndex: zIndex,
        top: pos + "px",
    }
}))`
  height: ${c.SCOREBOARD_ROW_HEIGHT}px;
  transition: top ${c.SCOREBOARD_ROW_TRANSITION_TIME}ms ease-in-out;
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

export const extractScoreboardRows = (data, selectedGroup) =>
    data.rows.filter(t => selectedGroup === "all" || (t?.teamGroups ?? []).includes(selectedGroup));

/**
 * Scollbar for scoreboard
 * @param {number} totalRows - total number of rows in scoreboard
 * @param {number} singleScreenRowCount - total number of rows that can fit on a single screen
 * @param {number} scrollInterval - interval of scrolling
 * @param {number} startFromRow - row to start from inclusive
 * @param {number} numRows - row to end to inclusive
 */
export const useScroller = (
    totalRows,
    singleScreenRowCount,
    scrollInterval,
    startFromRow,
    numRows
) => {
    const showRows = numRows ? numRows : totalRows;
    const numPages = Math.ceil(showRows / singleScreenRowCount);
    const singlePageRowCount = Math.floor(showRows / numPages);
    const [curPage, setCurPage] = useState(0);
    useEffect(() => {
        const intervalId = setInterval(() => {
            setCurPage((page) => (page + 1) % numPages);
        }, scrollInterval);
        return () => {
            clearInterval(intervalId);
        };
    }, [scrollInterval, numPages]);
    const pageEndRow = Math.min((curPage + 1) * singlePageRowCount + startFromRow, totalRows);
    return Math.max(startFromRow, pageEndRow - singleScreenRowCount);
};

export const ScoreboardRows = ({ settings }) => {
    const rows = extractScoreboardRows(
        useSelector((state) => state.scoreboard[settings.optimismLevel]),
        settings.group);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    const rowHeight = c.SCOREBOARD_ROW_HEIGHT;
    const scrollPos = useScroller(rows.length, settings.teamsOnPage, c.SCOREBOARD_SCROLL_INTERVAL, settings.startFromRow - 1, settings.numRows);
    return <ScoreboardRowsWrap>
        {teams.map(([index, teamData]) =>
            <PositionedScoreboardRow key={teamData.teamId} zIndex={rows.length-index} pos={(index - scrollPos) * (rowHeight + c.SCOREBOARD_ROW_PADDING) - c.SCOREBOARD_ROW_PADDING}>
                <ScoreboardRow teamId={teamData.teamId} optimismLevel={settings.optimismLevel}/>
            </PositionedScoreboardRow>
        )}
    </ScoreboardRowsWrap>;
};

const ScoreboardTableHeaderWrap = styled(ScoreboardTableRowWrap)`
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  height: ${c.SCOREBOARD_HEADER_HEIGHT};

  font-size: ${c.SCOREBOARD_HEADER_FONT_SIZE};
  font-style: normal;
  font-weight: ${c.SCOREBOARD_HEADER_FONT_WEIGHT};
  line-height: ${c.SCOREBOARD_HEADER_HEIGHT};
`;

const ScoreboardTableHeaderCell = styled.div`
  text-align: center;
  background-color: ${c.SCOREBOARD_HEADER_BACKGROUND_COLOR};
  padding: 0 8px;
`;

const ScoreboardTableHeaderNameCell = styled(ScoreboardTableHeaderCell)`
  text-align: left;
`;


const ScoreboardProblemLabel = styled(ProblemLabel)`
  width: unset;
`;

const ScoreboardTableHeader = () => {
    const problems = useSelector((state) => state.contestInfo.info?.problems);
    const needPenalty = useNeedPenalty();
    return <ScoreboardTableHeaderWrap nProblems={problems?.length ?? 1}>
        <ScoreboardTableHeaderCell>#</ScoreboardTableHeaderCell>
        <ScoreboardTableHeaderNameCell>Name</ScoreboardTableHeaderNameCell>
        <ScoreboardTableHeaderCell>Σ</ScoreboardTableHeaderCell>
        {needPenalty && <ScoreboardTableHeaderCell>Penalty</ScoreboardTableHeaderCell>}
        {problems && problems.map((probData) => <ScoreboardProblemLabel key={probData.name} letter={probData.letter}
            problemColor={probData.color}/>
        )}
    </ScoreboardTableHeaderWrap>;
};

export const Scoreboard = ({ widgetData: { settings } }) => {
    return <ScoreboardWrap>
        <ScoreboardHeader>
            <ScoreboardTitle>
                {nameTable[settings.optimismLevel] ?? "??"} standings
            </ScoreboardTitle>
            <ScoreboardCaption>
                {c.SCOREBOARD_CAPTION}
            </ScoreboardCaption>
        </ScoreboardHeader>
        <ScoreboardContent>
            <ScoreboardTableHeader/>
            <ScoreboardRows settings={settings}/>
        </ScoreboardContent>
    </ScoreboardWrap>;
};

Scoreboard.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard;
