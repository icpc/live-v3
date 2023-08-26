import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_BG_COLOR,
    SCOREBOARD_HEADER_BG_COLOR,
    SCOREBOARD_HEADER_TITLE_BG_COLOR,
    SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR,
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
import {
    formatPenalty,
    formatScore,
    needPenalty,
    ProblemCell,
    RankCell,
    TextShrinkingCell
} from "../../atoms/ContestCells";
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


const nameTable = {
    normal: "CURRENT",
    optimistic: "OPTIMISTIC",
    pessimistic: "PESSIMISTIC",
};

const ScoreboardRowContainer = styled.div`
  height: 100%;
  width: 100%;
  display: flex;
  overflow: hidden;
  /* box-sizing: border-box; */
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

const mapNumber = (value, oldMin, oldMax, newMin, newMax) => {
    const result = (value - oldMin) * (newMax - newMin) / (oldMax - oldMin) + newMin;
    return Math.min(Math.max(result, newMin), newMax);
};

// Green color: #1B8041, RGB(27, 128, 65) (VERDICT_OK)
// Red color: #881f1b, RGB(136, 31, 27) (VERDICT_NOK)
export const getTeamTaskColor = (score, minScore, maxScore) => {
    if (score === undefined) {
        return CELL_BG_COLOR;
    }
    if (minScore !== undefined && maxScore !== undefined) {
        const [minRed, minGreen, minBlue] = [136, 31, 27];
        const [maxRed, maxGreen, maxBlue] = [27, 128, 65];

        const scoreDiff = maxScore - minScore;
        const redDiff = maxRed - minRed;
        const greenDiff = maxGreen - minGreen;
        const blueDiff = maxBlue - minBlue;

        const middleRange = mapNumber(score, minScore, maxScore, 0, Math.PI);
        const middleFactor = 90;

        const [red, green, blue] = [
            Math.min(minRed + score * (redDiff / scoreDiff) + (middleFactor * Math.sin(middleRange)), 255),
            Math.min(minGreen + score * (greenDiff / scoreDiff) + (middleFactor * Math.sin(middleRange)), 255),
            Math.min(minBlue + score * (blueDiff / scoreDiff) + ((middleFactor * Math.sin(middleRange)) / 10), 255)
        ];

        return `#${((1 << 24) + (red << 16) + (green << 8) + blue).toString(16).slice(1, 7)}`;
    }

    return undefined;
};

function getStatusICPC(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
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

const ScoreboardICPCTaskCell = ({ status, attempts }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {TeamTaskSymbol[status]}
        {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
    </ScoreboardTaskCellWrap>;
};

export const ScoreboardIOITaskCell = ({ score, isFirstBest, minScore, maxScore, ...props }) => {
    return <ScoreboardTaskCellWrap background={getTeamTaskColor(score, minScore, maxScore)} {...props}>
        {isFirstBest && <StarIcon/>}
        {formatScore(score)}
    </ScoreboardTaskCellWrap>;
};

ScoreboardICPCTaskCell.propTypes = {
    status: PropTypes.oneOf(Object.values(TeamTaskStatus)),
    attempts: PropTypes.number
};

ScoreboardIOITaskCell.propTypes = {
    isFirstBest: PropTypes.bool,
    score: PropTypes.number,
    attempts: PropTypes.number,
    minScore: PropTypes.number,
    maxScore: PropTypes.number
};

const RenderScoreboardTaskCell = ({ data, ...props }) => {
    if (data.type === "ICPC") {
        return <ScoreboardICPCTaskCell
            status={getStatusICPC(data.isFirstToSolve, data.isSolved, data.pendingAttempts, data.wrongAttempts)}
            attempts={data.wrongAttempts + data.pendingAttempts}
            {...props}
        />;
    } else {
        return <ScoreboardIOITaskCell score={data.score} isFirstBest={data.isFirstBest} {...props} />;
    }
};

RenderScoreboardTaskCell.propTypes = {
    data: PropTypes.object
};

const ScoreboardHeaderWrap = styled(ScoreboardRowContainer)`
  height: ${props => props.rowHeight}px;
`;

const ScoreboardHeaderTitle = styled(ScoreboardCell).attrs(({ color }) => ({
    style: {
        background: color
    }
}))`
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

export const ScoreboardRow = ({ teamId, hideTasks, rankWidth, nameWidth, sumPenWidth, nameGrows, optimismLevel }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[optimismLevel].ids[teamId]);
    const contestData = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    return <ScoreboardRowContainer>
        <RankCell rank={scoreboardData?.rank} medal={scoreboardData?.medalType}
            width={rankWidth ?? SCOREBOARD_RANK_WIDTH}/>
        <TextShrinkingCell text={teamData?.shortName}
            width={nameGrows ? undefined : (nameWidth ?? SCOREBOARD_NAME_WIDTH)}
            canGrow={nameGrows ?? false} canShrink={nameGrows ?? false}/>
        <ScoreboardStatCell width={sumPenWidth ?? SCOREBOARD_SUM_PEN_WIDTH}>
            {scoreboardData === null ? null : formatScore(scoreboardData.totalScore)}
        </ScoreboardStatCell>
        {needPenalty(contestData) && <ScoreboardStatCell width={sumPenWidth ?? SCOREBOARD_SUM_PEN_WIDTH}>
            {scoreboardData === null ? null : formatPenalty(contestData, scoreboardData.penalty)}
        </ScoreboardStatCell>}
        {!hideTasks && scoreboardData?.problemResults.map((resultsData, i) =>
            <RenderScoreboardTaskCell key={i}  data={resultsData} minScore={contestData?.problems[i]?.minScore} maxScore={contestData?.problems[i]?.maxScore} />
        )}
    </ScoreboardRowContainer>;
};
ScoreboardRow.propTypes = {
    teamId: PropTypes.number.isRequired,
    hideTasks: PropTypes.bool
};

const ScoreboardHeader = ({ problems, rowHeight, name }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);

    let color = SCOREBOARD_HEADER_TITLE_BG_COLOR;
    if (name === "optimistic") {
        color = SCOREBOARD_HEADER_TITLE_BG_GREEN_COLOR;
    }
    return <ScoreboardHeaderWrap rowHeight={rowHeight}>
        <ScoreboardHeaderTitle color={color}>{nameTable[name]} STANDINGS</ScoreboardHeaderTitle>
        <ScoreboardHeaderStatCell>&#931;</ScoreboardHeaderStatCell>
        {needPenalty(contestInfo) && <ScoreboardHeaderStatCell>PEN</ScoreboardHeaderStatCell>}
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
  height: ${props => props.rowHeight + 2}px; /* FIXME lol */
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

const extractScoreboardRows = (data, selectedGroup) =>
    data.rows.filter(t => selectedGroup === "all" || (t?.teamGroups ?? []).includes(selectedGroup));

/**
 * Scollbar for scoreboard
 * @param {number} totalRows - total number of rows in scoreboard
 * @param {number} singleScreenRowCount - total number of rows that can fit on a single screen
 * @param {number} scrollInterval - interval of scrolling
 * @param {number} startFromRow - row to start from inclusive
 * @param {number} numRows - row to end to inclusive
 */
const useScoller = (totalRows,
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

export const Scoreboard = ({ widgetData: { settings, location } }) => {
    const optimismLevel = settings.optimismLevel;
    const teamsOnPage = settings.teamsOnPage;
    const startPageRow = settings.startFromRow - 1;
    const rows = extractScoreboardRows(
        useSelector((state) => state.scoreboard[optimismLevel]),
        settings.group);
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const totalHeight = location.sizeY;
    const rowHeight = (totalHeight / (teamsOnPage + 1));
    const scrollPos = useScoller(rows.length, teamsOnPage, SCOREBOARD_SCROLL_INTERVAL, startPageRow, settings.numRows);
    const teams = _(rows).toPairs().sortBy("[1].teamId").value();
    return <ScoreboardWrap>
        <ScoreboardHeader problems={contestInfo?.problems} rowHeight={rowHeight} name={optimismLevel} key={"header"}/>
        <div style={{ overflow: "hidden", height: "100%" }}>
            {teams.map(([ind, teamRowData]) =>
                <PositionedScoreboardRow key={teamRowData.teamId} pos={ind * rowHeight - scrollPos * rowHeight}
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
