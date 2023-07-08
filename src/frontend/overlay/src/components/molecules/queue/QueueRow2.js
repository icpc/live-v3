import PropTypes from "prop-types";
import React from "react";
import { useSelector } from "react-redux";
import {
    CELL_QUEUE_RANK_WIDTH2,
    CELL_QUEUE_TOTAL_SCORE_WIDTH,
    CELL_QUEUE_VERDICT_WIDTH2,
    CONTESTER_ROW_VERDICT_FONT_SIZE2,
    QUEUE_VERDICT_PADDING_LEFT2
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { formatScore } from "../../atoms/ContestCells";
import { ProblemCircleLabel, RankLabel2, RunStatusLabel2 } from "../../atoms/ContestLabels2";
import { FlexedBox2, ShrinkingBox2 } from "../../atoms/Box2";
import { ContesterRow2 } from "../../atoms/ContesterRow2";


export const QueueRow2 = ({ runInfo, isEven, flashing }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[runInfo.teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[runInfo.teamId]);
    const probData = useSelector((state) => state.contestInfo.info?.problemsId[runInfo.problemId]);

    return <ContesterRow2 medal={scoreboardData?.medalType} isEven={isEven} flashing={flashing}>
        <RankLabel2 width={CELL_QUEUE_RANK_WIDTH2} rank={scoreboardData?.rank} marginRight={"6px"}/>
        <ShrinkingBox2 text={teamData?.shortName ?? "??"} flexGrow={1} flexShrink={1} Wrapper={FlexedBox2}/>
        <ShrinkingBox2 width={CELL_QUEUE_TOTAL_SCORE_WIDTH} align={"center"} Wrapper={FlexedBox2}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
        />
        <ProblemCircleLabel letter={probData?.letter} problemColor={probData?.color} />
        <RunStatusLabel2
            runInfo={runInfo}
            width={CELL_QUEUE_VERDICT_WIDTH2}
            marginLeft={QUEUE_VERDICT_PADDING_LEFT2}
            marginTop={"6px"}
            fontSize={CONTESTER_ROW_VERDICT_FONT_SIZE2}
        />
    </ContesterRow2>;
};

QueueRow2.propTypes = {
    runInfo: PropTypes.object.isRequired,
    isEven: PropTypes.bool.isRequired
};

