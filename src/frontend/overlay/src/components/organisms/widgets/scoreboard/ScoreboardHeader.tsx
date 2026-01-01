import React from "react";
import styled from "styled-components";
import c from "@/config";
import { ProblemLabel } from "@/components/atoms/ProblemLabel";
import { ShrinkingBox } from "../../../atoms/ShrinkingBox";
import { useNeedPenalty } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";
import { OptimismLevel } from "@shared/api";

const ScoreboardHeaderWrap = styled.div`
    display: flex;
    flex-direction: row;

    width: 100%;
    padding-top: ${c.SCOREBOARD_HEADER_PADDING_TOP};

    font-size: ${c.SCOREBOARD_CAPTION_FONT_SIZE};
    font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    font-style: normal;
`;

const ScoreboardTitle = styled.div`
    flex: 1 0 0;
`;

const ScoreboardCaption = styled.div``;

const ScoreboardTableRowWrap = styled.div<{
    needPenalty: boolean;
    nProblems: number;
}>`
    display: grid;
    grid-template-columns:
        ${c.SCOREBOARD_CELL_PLACE_SIZE}
        ${c.SCOREBOARD_CELL_TEAMNAME_SIZE}
        ${c.SCOREBOARD_CELL_POINTS_SIZE}
        ${({ needPenalty }) =>
            needPenalty ? c.SCOREBOARD_CELL_PENALTY_SIZE : ""}
        repeat(${(props) => props.nProblems}, 1fr);
    gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;

    box-sizing: border-box;

    background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
`;

const ScoreboardTableHeaderWrap = styled(ScoreboardTableRowWrap)`
    overflow: hidden;

    height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

    font-size: ${c.SCOREBOARD_HEADER_FONT_SIZE};
    font-weight: ${c.SCOREBOARD_HEADER_FONT_WEIGHT};
    font-style: normal;
    line-height: ${c.SCOREBOARD_HEADER_HEIGHT}px;

    border-radius: ${c.SCOREBOARD_HEADER_BORDER_RADIUS_TOP_LEFT}
        ${c.SCOREBOARD_HEADER_BORDER_RADIUS_TOP_RIGHT} 0 0;
`;

const ScoreboardTableHeaderCell = styled.div`
    padding: 0 ${c.SCOREBOARD_CELL_PADDING};
    text-align: center;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    background-color: ${c.SCOREBOARD_HEADER_BACKGROUND_COLOR};
`;

const ScoreboardTableHeaderNameCell = styled(ScoreboardTableHeaderCell)`
    text-align: left;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
`;

const ScoreboardProblemLabel = styled(ProblemLabel)`
    width: unset;
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
`;

export const nameTable = {
    normal: c.SCOREBOARD_NORMAL_NAME,
    optimistic: c.SCOREBOARD_OPTIMISTIC_NAME,
    pessimistic: c.SCOREBOARD_PESSIMISTIC_NAME,
};

interface ScoreboardHeaderProps {
    optimismLevel: OptimismLevel;
}

export function ScoreboardTableHeader() {
    const problems = useAppSelector(
        (state) => state.contestInfo.info?.problems,
    );
    const needPenalty = useNeedPenalty();

    return (
        <ScoreboardTableHeaderWrap
            nProblems={Math.max(problems?.length ?? 0, 1)}
            needPenalty={needPenalty}
        >
            <ScoreboardTableHeaderCell>#</ScoreboardTableHeaderCell>
            <ScoreboardTableHeaderNameCell>Name</ScoreboardTableHeaderNameCell>
            <ScoreboardTableHeaderCell>Σ</ScoreboardTableHeaderCell>
            {needPenalty && (
                <ScoreboardTableHeaderCell>
                    <ShrinkingBox text={"Penalty"} />
                </ScoreboardTableHeaderCell>
            )}
            {problems &&
                problems.map((probData) => (
                    <ScoreboardProblemLabel
                        key={probData.name}
                        letter={probData.letter}
                        problemColor={probData.color}
                    />
                ))}
        </ScoreboardTableHeaderWrap>
    );
}

export function ScoreboardHeader({ optimismLevel }: ScoreboardHeaderProps) {
    return (
        <ScoreboardHeaderWrap>
            <ScoreboardTitle>
                {nameTable[optimismLevel] ?? c.SCOREBOARD_UNDEFINED_NAME}{" "}
                {c.SCOREBOARD_STANDINGS_NAME}
            </ScoreboardTitle>
            <ScoreboardCaption>{c.SCOREBOARD_CAPTION}</ScoreboardCaption>
        </ScoreboardHeaderWrap>
    );
}
