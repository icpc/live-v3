import React from "react";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import {
    CELL_QUEUE_RANK_WIDTH,
    CELL_QUEUE_TOTAL_SCORE_WIDTH,
    QUEUE_PER_COLUMNS_PADDING2
} from "../../../config";
import { FlexedBox2, ShrinkingBox2 } from "../../atoms/Box2";
import { ContesterRow2 } from "../../atoms/ContesterRow2";
import { RankLabel2 } from "../../atoms/ContestLabels2";
import { formatScore } from "../../atoms/ContestCells";

export const ContesterInfo2 = ({ teamId, ...props }) => {
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    return <ContesterRow2 medal={scoreboardData?.medalType} {...props}>
        <RankLabel2 width={CELL_QUEUE_RANK_WIDTH} rank={scoreboardData?.rank}/>
        <ShrinkingBox2 width={"342px"} text={teamData?.shortName ?? "??"} flexGrow={1} flexShrink={1} Wrapper={FlexedBox2}
            marginLeft={QUEUE_PER_COLUMNS_PADDING2} marginRight={QUEUE_PER_COLUMNS_PADDING2}/>
        <ShrinkingBox2 width={CELL_QUEUE_TOTAL_SCORE_WIDTH} align={"center"} Wrapper={FlexedBox2}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}
            marginRight={QUEUE_PER_COLUMNS_PADDING2}
        />
        <ShrinkingBox2 width={CELL_QUEUE_TOTAL_SCORE_WIDTH} align={"center"} Wrapper={FlexedBox2}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.penalty ?? 0.0, 1)}
            marginRight={QUEUE_PER_COLUMNS_PADDING2}
        />
    </ContesterRow2>;
};
