import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { TaskResultLabel, RankLabel } from "../../atoms/ContestLabels";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";

import { formatScore, useFormatPenalty, useNeedPenalty } from "@/services/displayUtils";
import { useElementSize } from "usehooks-ts";
import { useAppSelector } from "@/redux/hooks";
import { Award, OptimismLevel, ScoreboardSettings } from "@shared/api";
import { SCOREBOARD_TYPES } from "@/consts";


const ScoreboardWrap = styled.div`
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 14px;

  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 7px 16px 0 16px;

  color: ${c.SCOREBOARD_TEXT_COLOR};

  background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
  border-radius: ${c.SCOREBOARD_BORDER_RADIUS};
`;

const ScoreboardHeader = styled.div`
  display: flex;
  flex-direction: row;

  width: 100%;
  padding-top: 0.3em;

  font-size: ${c.SCOREBOARD_CAPTION_FONT_SIZE};
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  font-style: normal;
`;

const ScoreboardTitle = styled.div`
  flex: 1 0 0;
`;

const ScoreboardCaption = styled.div`
`;

const ScoreboardContent = styled.div`
  display: flex;
  flex: 1 0 0;
  flex-direction: column;
  gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;
`;


export const nameTable = {
    normal: c.SCOREBOARD_NORMAL_NAME,
    optimistic: c.SCOREBOARD_OPTIMISTIC_NAME,
    pessimistic: c.SCOREBOARD_PESSIMISTIC_NAME,
};

const ScoreboardTableRowWrap = styled.div<{needPenalty: boolean, nProblems: number}>`
  display: grid;
  grid-template-columns:
          ${c.SCOREBOARD_CELL_PLACE_SIZE}
          ${c.SCOREBOARD_CELL_TEAMNAME_SIZE} 
          ${c.SCOREBOARD_CELL_POINTS_SIZE} 
          ${({ needPenalty }) => needPenalty ? c.SCOREBOARD_CELL_PENALTY_SIZE : ""} 
          repeat(${props => props.nProblems}, 1fr);
  gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;

  box-sizing: border-box;

  background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
`;

const ScoreboardRowWrap = styled(ScoreboardTableRowWrap)`
  overflow: hidden;
  align-items: center;

  box-sizing: content-box;
  height: ${c.SCOREBOARD_ROW_HEIGHT}px;
  
  font-size: ${c.SCOREBOARD_ROW_FONT_SIZE};
  font-weight: ${c.SCOREBOARD_TABLE_ROW_FONT_WEIGHT};
  font-style: normal;

  border-top: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
  border-bottom: ${c.SCOREBOARD_ROWS_DIVIDER_COLOR} solid 1px;
`;

const ScoreboardRowName = styled(ShrinkingBox)`
  padding: 0 8px;

  /* font-weight: 700; */
`;

const ScoreboardRankLabel = styled(RankLabel)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-content: center;
`;
export const ScoreboardTaskResultLabel = styled(TaskResultLabel)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-content: center;
`;


interface ScoreboardRowProps {
    teamId: string,
    hideTasks?: boolean,
    optimismLevel: OptimismLevel
}

export const ScoreboardRow = ({ teamId,
    hideTasks = false, // wtf is this?
    optimismLevel }: ScoreboardRowProps) => {
    const scoreboardData = useAppSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const contestData = useAppSelector((state) => state.contestInfo.info);
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const awards: Award[] = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].idAwards[teamId]);
    const rank = useAppSelector((state) => teamId in state.scoreboard[SCOREBOARD_TYPES.normal].rankById && state.scoreboard[SCOREBOARD_TYPES.normal].rankById[teamId]);
    const medal = awards?.find((award) => award.type == Award.Type.medal) as Award.medal;
    const needPenalty = useNeedPenalty();
    const formatPenalty = useFormatPenalty();
    return <ScoreboardRowWrap nProblems={contestData?.problems?.length ?? 1} needPenalty={needPenalty}>
        <ScoreboardRankLabel rank={rank} medal={medal?.medalColor}/>
        <ScoreboardRowName align={c.SCOREBOARD_CELL_TEAMNANE_ALIGN} text={teamData?.shortName ?? "??"}/>
        <ShrinkingBox align={c.SCOREBOARD_CELL_POINTS_ALIGN}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}/>
        {needPenalty && <ShrinkingBox align={c.SCOREBOARD_CELL_PENALTY_ALIGN} text={
            formatPenalty(scoreboardData?.penalty)
        } />}
        {!hideTasks && scoreboardData?.problemResults.map((result, i) =>
            <ScoreboardTaskResultLabel problemResult={result} key={i} problemColor={contestData?.problems[i]?.color}
                minScore={contestData?.problems[i]?.minScore} maxScore={contestData?.problems[i]?.maxScore}/>
        )}
    </ScoreboardRowWrap>;
};

type PositionedScoreboardRowProps = {
    zIndex: number,
    pos: number,
}

const PositionedScoreboardRow = styled.div.attrs<PositionedScoreboardRowProps>(({ zIndex, pos }) => ({
    style: {
        zIndex: zIndex,
        top: pos + "px",
    }
}))<PositionedScoreboardRowProps>`
  position: absolute;
  right: 0;
  left: 0;

  width: 100%;
  height: ${c.SCOREBOARD_ROW_HEIGHT}px;

  transition: top ${c.SCOREBOARD_ROW_TRANSITION_TIME}ms ease-in-out;
`;

const ScoreboardRowsWrap = styled.div<{maxHeight: number}>`
  position: relative;

  overflow: hidden;
  flex: 1 0 0;

  height: auto;
  max-height: ${({ maxHeight }) => `${maxHeight}px`};
`;

export const useScoreboardRows = (optimismLevel: OptimismLevel, selectedGroup: string) => {
    const order = useAppSelector((state) => state.scoreboard[optimismLevel]?.orderById);
    const teamsId = useAppSelector((state) => state.contestInfo.info?.teamsId);
    if (teamsId === undefined || order === undefined) {
        return [];
    }
    return Object.entries(order)
        .filter(([k]) =>
            selectedGroup === "all" || (teamsId[k]?.groups ?? []).includes(selectedGroup));
};

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

interface ScoreboardRowsProps {
    settings: ScoreboardSettings,
    onPage: number
}

export const ScoreboardRows = ({ settings, onPage }: ScoreboardRowsProps) => {
    const rows = useScoreboardRows(settings.optimismLevel, settings.group);
    const rowHeight = c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING;
    const scrollPos = useScroller(rows.length, onPage, c.SCOREBOARD_SCROLL_INTERVAL, settings.startFromRow - 1, settings.numRows);
    return <ScoreboardRowsWrap maxHeight={onPage * rowHeight}>
        {rows.map(([teamId, position]) =>
            <PositionedScoreboardRow key={teamId} zIndex={rows.length-position} pos={(position - scrollPos) * rowHeight - c.SCOREBOARD_ROW_PADDING}>
                <ScoreboardRow teamId={teamId} optimismLevel={settings.optimismLevel}/>
            </PositionedScoreboardRow>
        )}
    </ScoreboardRowsWrap>;
};

const ScoreboardTableHeaderWrap = styled(ScoreboardTableRowWrap)`
  overflow: hidden;

  height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

  font-size: ${c.SCOREBOARD_HEADER_FONT_SIZE};
  font-weight: ${c.SCOREBOARD_HEADER_FONT_WEIGHT};
  font-style: normal;
  line-height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

  border-radius: 16px 16px 0 0;
`;

const ScoreboardTableHeaderCell = styled.div`
  padding: 0 8px;
  text-align: center;
  background-color: ${c.SCOREBOARD_HEADER_BACKGROUND_COLOR};
`;

const ScoreboardTableHeaderNameCell = styled(ScoreboardTableHeaderCell)`
  text-align: left;
`;


const ScoreboardProblemLabel = styled(ProblemLabel)`
  width: unset;
`;

const ScoreboardTableHeader = () => {
    const problems = useAppSelector((state) => state.contestInfo.info?.problems);
    const needPenalty = useNeedPenalty();
    return <ScoreboardTableHeaderWrap nProblems={problems?.length ?? 1} needPenalty={needPenalty}>
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
    const [rowsRef, { height }] = useElementSize();
    const onPage = Math.floor((height - c.SCOREBOARD_HEADER_HEIGHT) / (c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING));

    return <ScoreboardWrap>
        <ScoreboardHeader>
            <ScoreboardTitle>
                {nameTable[settings.optimismLevel] ?? c.SCOREBOARD_UNDEFINED_NAME} {c.SCOREBOARD_STANDINGS_NAME}
            </ScoreboardTitle>
            <ScoreboardCaption>
                {c.SCOREBOARD_CAPTION}
            </ScoreboardCaption>
        </ScoreboardHeader>
        <ScoreboardContent ref={rowsRef}>
            <ScoreboardTableHeader/>
            <ScoreboardRows settings={settings} onPage={onPage} />
        </ScoreboardContent>
    </ScoreboardWrap>;
};

Scoreboard.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard;
