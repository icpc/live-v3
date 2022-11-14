import PropTypes from "prop-types";
import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_RANK_WIDTH,
    CELL_QUEUE_TASK_WIDTH,
    CELL_QUEUE_TOTAL_SCORE_WIDTH,
    CELL_QUEUE_VERDICT_WIDTH,
    QUEUE_OPACITY,
    QUEUE_ROW_HEIGHT
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, RankCell, TextShrinkingCell, VerdictCell } from "../../atoms/ContestCells";
import { formatScore } from "../../atoms/ContestCells";


const QueueRowWrap = styled.div`
  height: ${QUEUE_ROW_HEIGHT}px;
  display: flex;
  flex-wrap: nowrap;
  max-width: 100%;
  opacity: ${QUEUE_OPACITY};
`;

export const QueueRow = ({ entryData, isEven, flash }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[entryData.teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[entryData.teamId]);
    const probData = useSelector((state) => state.contestInfo.info?.problemsId[entryData.problemId]);
    return <QueueRowWrap>
        <RankCell width={CELL_QUEUE_RANK_WIDTH} isEven={isEven} rank={scoreboardData?.rank}
            medal={scoreboardData?.medalType} flash={flash}/>
        <TextShrinkingCell text={teamData?.shortName ?? "??"} isEven={isEven} flash={flash}/>
        <Cell width={CELL_QUEUE_TOTAL_SCORE_WIDTH} isEven={isEven} flash={flash}>
            {scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore, 1)}
        </Cell>
        <ProblemCell probData={probData} width={CELL_QUEUE_TASK_WIDTH} isEven={isEven} flash={flash}/>
        <VerdictCell verdict={entryData} width={CELL_QUEUE_VERDICT_WIDTH} isEven={isEven} flash={flash}/>
    </QueueRowWrap>;
};

QueueRow.propTypes = {
    entryData: PropTypes.object.isRequired,
    isEven: PropTypes.bool.isRequired
};

