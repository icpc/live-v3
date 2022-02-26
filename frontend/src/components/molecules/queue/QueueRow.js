import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_RANK_WIDTH,
    CELL_QUEUE_TASK_WIDTH,
    CELL_QUEUE_TOTAL_SCORE_WIDTH,
    CELL_QUEUE_VERDICT_WIDTH,
    QUEUE_ROW_HEIGHT
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, TeamNameCell, VerdictCell } from "../ContestCells";


const QueueRowWrap = styled.div`
  height: ${QUEUE_ROW_HEIGHT}px;
  display: flex;
  flex-wrap: nowrap;
  max-width: 100%;
`;

export const QueueRow = ({ entryData }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[entryData.teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[entryData.teamId]);
    return <QueueRowWrap>
        <Cell width={CELL_QUEUE_RANK_WIDTH}>
            {scoreboardData?.rank ?? "??"}
        </Cell>
        <TeamNameCell teamName={teamData?.name ?? "??"}/>
        <Cell width={CELL_QUEUE_TOTAL_SCORE_WIDTH}>
            {scoreboardData?.totalScore ?? "??"}
        </Cell>
        <ProblemCell probInd={entryData.problemId} width={CELL_QUEUE_TASK_WIDTH}/>
        <VerdictCell verdict={entryData} width={CELL_QUEUE_VERDICT_WIDTH}/>
    </QueueRowWrap>;
};

