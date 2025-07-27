import { useEffect, useState, useRef } from "react";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { TaskResultLabel, RankLabel } from "../../atoms/ContestLabels";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";

import { formatScore, useFormatPenalty, useNeedPenalty } from "@/services/displayUtils";
import { useResizeObserver } from "usehooks-ts";
import { useAppSelector } from "@/redux/hooks";
import { Award, ScoreboardSettings, OptimismLevel, Widget, ScoreboardScrollDirection } from "@shared/api";
import { OverlayWidgetC } from "@/components/organisms/widgets/types";


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
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};  
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
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};  
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
    const awards: Award[] = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].idAwards[teamId]);
    const rank = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].rankById[teamId]);
    const medal = awards?.find((award) => award.type == Award.Type.medal) as Award.medal;
    const needPenalty = useNeedPenalty();
    const formatPenalty = useFormatPenalty();
    return <ScoreboardRowWrap nProblems={Math.max(contestData?.problems?.length ?? 0, 1)} needPenalty={needPenalty}>
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


/**
 * Returns a stable list of teams with each having their row number in the scoreboard.
 * @param optimismLevel
 * @param selectedGroup
 */
export const useScoreboardRows = (optimismLevel: OptimismLevel, selectedGroup: string) => {
    const order = useAppSelector((state) => state.scoreboard[optimismLevel]?.orderById);
    const teamsId = useAppSelector((state) => state.contestInfo.info?.teamsId);
    if (teamsId === undefined || order === undefined) {
        return [];
    }
    const result = Object.entries(order).filter(([k]) => selectedGroup === "all" || (teamsId[k]?.groups ?? []).includes(selectedGroup));
    if (selectedGroup !== "all") { // we should compress the row numbers.
        // FIXME: this is ugly and I don't like it at all
        const rowsNumbers = result.map(([_, b]) => b);
        rowsNumbers.sort((a, b) => a > b ? 1 : a == b ? 0 : -1);
        const mapping = new Map();
        for (let i = 0; i<rowsNumbers.length; i++) {
            mapping.set(rowsNumbers[i], i);
        }
        for (let i = 0; i<result.length; i++) {
            result[i][1] = mapping.get(result[i][1]);
        }
    }
    return result;
};

/**
 * Scollbar for scoreboard
 * @param {number} totalRows - total number of rows in scoreboard
 * @param {number} singleScreenRowCount - total number of rows that can fit on a single screen
 * @param {number} interval - interval of scrolling
 * @param {number | undefined} direction - row to start from inclusive
 * @returns {number} - index of the first row to show
 */
export const useScroller = (
    totalRows: number,
    singleScreenRowCount: number,
    interval: number,
    direction: ScoreboardScrollDirection | undefined,
) => {
    const showRows = totalRows;
    const numPages = Math.ceil(showRows / singleScreenRowCount);
    const singlePageRowCount = Math.ceil(showRows / numPages);
    const [curPage, setCurPage] = useState(0);
    useEffect(() => {
        if (direction === ScoreboardScrollDirection.FirstPage) {
            setCurPage(0);
        } else if (direction === ScoreboardScrollDirection.LastPage) {
            setCurPage(numPages - 1);
        } else if (direction !== ScoreboardScrollDirection.Pause) {
            const intervalId = setInterval(() => {
                setCurPage(page => Math.max(0, (page + (direction === ScoreboardScrollDirection.Back ? -1 : 1)) % numPages));
            }, interval);
            return () => {
                clearInterval(intervalId);
            };
        }
    }, [interval, numPages, direction]);
    const pageEndRow = Math.min((curPage + 1) * singlePageRowCount, totalRows);
    return Math.max(0, pageEndRow - singleScreenRowCount);
};

interface ScoreboardRowsProps {
    settings: ScoreboardSettings,
    onPage: number
}

export const ScoreboardRows = ({ settings, onPage }: ScoreboardRowsProps) => {
    const rows = useScoreboardRows(settings.optimismLevel, settings.group);
    const rowHeight = c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING;
    const scrollPos = useScroller(rows.length, onPage, c.SCOREBOARD_SCROLL_INTERVAL, settings.scrollDirection);
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
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};  
  background-color: ${c.SCOREBOARD_HEADER_BACKGROUND_COLOR};
`;

const ScoreboardTableHeaderNameCell = styled(ScoreboardTableHeaderCell)`
  text-align: left;
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY}
`;


const ScoreboardProblemLabel = styled(ProblemLabel)`
  width: unset;
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};  
`;

const ScoreboardTableHeader = () => {
    const problems = useAppSelector((state) => state.contestInfo.info?.problems);
    const needPenalty = useNeedPenalty();
    return <ScoreboardTableHeaderWrap nProblems={Math.max(problems?.length ?? 0, 1)} needPenalty={needPenalty}>
        <ScoreboardTableHeaderCell>#</ScoreboardTableHeaderCell>
        <ScoreboardTableHeaderNameCell>Name</ScoreboardTableHeaderNameCell>
        <ScoreboardTableHeaderCell>Σ</ScoreboardTableHeaderCell>
        {needPenalty && <ScoreboardTableHeaderCell><ShrinkingBox text={"Penalty"}/></ScoreboardTableHeaderCell>}
        {problems && problems.map((probData) => <ScoreboardProblemLabel key={probData.name} letter={probData.letter}
            problemColor={probData.color}/>
        )}
    </ScoreboardTableHeaderWrap>;
};

export const Scoreboard: OverlayWidgetC<Widget.ScoreboardWidget> = ({ widgetData: { settings } }) => {
    const ref = useRef<HTMLDivElement>(null);
    const { height = 0 } = useResizeObserver({ ref });
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
        <ScoreboardContent ref={ref}>
            <ScoreboardTableHeader/>
            <ScoreboardRows settings={settings} onPage={onPage} />
        </ScoreboardContent>
    </ScoreboardWrap>;
};

export default Scoreboard;
