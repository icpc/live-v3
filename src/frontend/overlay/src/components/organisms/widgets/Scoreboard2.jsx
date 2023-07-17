import _ from "lodash";
import PropTypes from "prop-types";
import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_TASK_WIDTH,
    QUEUE_PER_COLUMNS_PADDING2,
    SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR,
    SCOREBOARD_NAME_WIDTH2,
    SCOREBOARD_RANK_WIDTH2,
    SCOREBOARD_ROW_TRANSITION_TIME,
    SCOREBOARD_SCROLL_INTERVAL,
    WIDGET_LAYOUT_BACKGROUND,
} from "../../../config";
import { formatScore } from "../../atoms/ContestCells";
import { extractScoreboardRows, nameTable, useScroller } from "./Scoreboard";
import { ContestantRow2 } from "../../atoms/ContestantRow2";
import { TaskResultLabel2, RankLabel } from "../../atoms/ContestLabels2";
import { FlexedBox2, ShrinkingBox2 } from "../../atoms/Box2";
import { CircleCell } from "../../atoms/CircleCellsProblem";


const ScoreboardWrap2 = styled.div`
  height: 100%;
  width: 100%;
  border: none;
  border-collapse: collapse;
  table-layout: fixed;
  display: flex;
  flex-direction: column;
  background-color: ${WIDGET_LAYOUT_BACKGROUND};
  border-radius: 16px;
  overflow: hidden;
`;


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

const PositionedScoreboardRowWrap = styled.div.attrs(({ zIndex, pos }) => ({
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

const PositionedScoreboardRow = ({ zIndex, children, ...props }) => {
    return <PositionedScoreboardRowWrap zIndex={zIndex} {...props}>
        {children}
    </PositionedScoreboardRowWrap>;
};

PositionedScoreboardRow.propTypes = {
    zIndex: PropTypes.number,
    children: PropTypes.node
};

const ScoreboardHeaderTitle2 = styled(FlexedBox2)`
  text-align: center;
  width: calc(${SCOREBOARD_RANK_WIDTH2} + ${SCOREBOARD_NAME_WIDTH2});
`;

const ScoreboardProblemLabel2 = ({ probData: prob }) =>
    <FlexedBox2 flexGrow={1} flexShrink={1} flexBasis={0} align={"center"} display="flex">
        <CircleCell content={prob?.letter ?? "??"} backgroundColor={prob?.color ?? "black"} width={CELL_QUEUE_TASK_WIDTH}/>
    </FlexedBox2>;

const ScoreboardHeader2 = ({ problems, name }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    let color = name === "optimistic" ? SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR : undefined;

    return <ContestantRow2>
        <ScoreboardHeaderTitle2 color={color} marginLeft={QUEUE_PER_COLUMNS_PADDING2} marginRight={QUEUE_PER_COLUMNS_PADDING2}>
            {nameTable[name] + " STANDINGS"}
        </ScoreboardHeaderTitle2>
        {/*marginRight={QUEUE_PER_COLUMNS_PADDING2}*/}
        <FlexedBox2 flexGrow={1} flexShrink={1} flexBasis={0} align={"center"}>&#931;</FlexedBox2>
        {contestInfo?.resultType === "ICPC" && <FlexedBox2 flexGrow={1} flexShrink={1} flexBasis={0} align={"center"}>PEN</FlexedBox2>}
        {problems && problems.map((probData) => <ScoreboardProblemLabel2 probData={probData} key={probData.name}/>)}
    </ContestantRow2>;
};

ScoreboardHeader2.propTypes = {
    problems: PropTypes.arrayOf(PropTypes.object)
};

export const Scoreboard2 = ({ widgetData: { settings, location } }) => {
    const optimismLevel = settings.optimismLevel;
    const teamsOnPage = settings.teamsOnPage;
    const startPageRow = settings.startFromRow - 1;
    const rows = extractScoreboardRows(
        useSelector((state) => state.scoreboard[optimismLevel]),
        settings.group);
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const totalHeight = location.sizeY;
    const rowHeight = (totalHeight / (teamsOnPage + 1));
    const scrollPos = useScroller(rows.length, teamsOnPage, SCOREBOARD_SCROLL_INTERVAL, startPageRow, settings.numRows);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    return <ScoreboardWrap2>
        <PositionedScoreboardRow pos={0} rowHeight={rowHeight} zIndex={rows.length + 1}>
            <ScoreboardHeader2 problems={contestInfo?.problems} rowHeight={rowHeight} name={optimismLevel} key={"header"}/>
        </PositionedScoreboardRow>
        {teams.map(([ind, teamRowData]) =>
            <PositionedScoreboardRow key={teamRowData.teamId} pos={ind * rowHeight - scrollPos * rowHeight + rowHeight}
                rowHeight={rowHeight} zIndex={rows.length - ind}>
                <ScoreboardRow2 teamId={teamRowData.teamId} optimismLevel={optimismLevel} isEven={ind % 2 === 0}/>
            </PositionedScoreboardRow>
        )}
    </ScoreboardWrap2>;
};

Scoreboard2.propTypes = {
    widgetData: PropTypes.object.isRequired
};

export default Scoreboard2;
