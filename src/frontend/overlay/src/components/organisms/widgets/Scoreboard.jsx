import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    SCOREBOARD_ROW_TRANSITION_TIME,
    SCOREBOARD_SCROLL_INTERVAL,
    SCOREBOARD_BACKGROUND_COLOR,
    SCOREBOARD_CAPTION,
    SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR,
    SCOREBOARD_TABLE_ROWS_DIVIDER_COLOR,
} from "../../../config";
import { formatPenalty, formatScore, useNeedPenalty } from "../../atoms/ContestCells";
import { ProblemLabel } from "../../atoms/ProblemLabel";
// import { extractScoreboardRows, useScroller } from "./Scoreboard";
import { TaskResultLabel, RankLabel } from "../../atoms/ContestLabels";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";


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
  gap: 3px;
`;


export const nameTable = {
    normal: "Current",
    optimistic: "Optimistic",
    pessimistic: "Pessimistic",
};

const ScoreboardTableRowWrap = styled.div`
  gap: 3px;
  box-sizing: border-box;
  background-color: ${SCOREBOARD_BACKGROUND_COLOR};
  display: grid;
  grid-template-columns: 73px 304px 81px 92px repeat(${props => props.nProblems}, 1fr);
`;

const ScoreboardRowWrap = styled(ScoreboardTableRowWrap)`
  height: 25px;
  overflow: hidden;
  box-sizing: content-box;
  border-top: ${SCOREBOARD_TABLE_ROWS_DIVIDER_COLOR} solid 3px;
  border-bottom: ${SCOREBOARD_TABLE_ROWS_DIVIDER_COLOR} solid 3px;

  text-align: center;
  font-size: 18px;
  font-style: normal;
  font-weight: 300;
  line-height: 25px; /* 183.333% */
`;

const ScoreboardRowName = styled(ShrinkingBox)`
  //font-weight: 700;
  padding: 0 8px;
`;

export const ScoreboardRow = ({ teamId, hideTasks, optimismLevel }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const contestData = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const needPenalty = useNeedPenalty();
    return <ScoreboardRowWrap medal={scoreboardData?.medalType} nProblems={contestData?.problems?.length ?? 1}>
        <RankLabel rank={scoreboardData?.rank} medal={scoreboardData?.medalType}/>
        <ScoreboardRowName align={"left"} text={teamData?.shortName ?? "??"}/>
        <ShrinkingBox align={"center"}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}/>
        {needPenalty && <ShrinkingBox align={"center"} text={
            formatPenalty(contestData, scoreboardData?.penalty)
        } />}
        {!hideTasks && scoreboardData?.problemResults.map((result, i) =>
            <TaskResultLabel problemResult={result} key={i}
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
  height: ${props => props.rowHeight + 2}px; /* FIXME lol */
  transition: top ${SCOREBOARD_ROW_TRANSITION_TIME}ms ease-in-out;
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
    const rowHeight = 25;
    const scrollPos = useScroller(rows.length, settings.teamsOnPage, c.SCOREBOARD_SCROLL_INTERVAL, settings.startFromRow - 1, settings.numRows);
    return <ScoreboardRowsWrap>
        {teams.map(([index, teamData]) =>
            <PositionedScoreboardRow key={teamData.teamId} zIndex={rows.length-index} pos={(index - scrollPos) * (rowHeight + 1) - 1}>
                <ScoreboardRow teamId={teamData.teamId} optimismLevel={settings.optimismLevel}/>
            </PositionedScoreboardRow>
        )}
    </ScoreboardRowsWrap>;
};

const ScoreboardTableHeaderWrap = styled(ScoreboardTableRowWrap)`
  // background-color: ${SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR};
  border-radius: 16px 16px 0 0;
  overflow: hidden;

  font-size: 21px;
  font-style: normal;
  font-weight: 700;
  line-height: 44px;
  
`;

const ScoreboardTableHeaderCell = styled.div`
  text-align: center;
  background-color: ${SCOREBOARD_TABLE_HEADER_BACKGROUND_COLOR};
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
        <ScoreboardTableHeaderCell>Place</ScoreboardTableHeaderCell>
        <ScoreboardTableHeaderNameCell>Team name</ScoreboardTableHeaderNameCell>
        <ScoreboardTableHeaderCell>Points</ScoreboardTableHeaderCell>
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
                {SCOREBOARD_CAPTION}
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
