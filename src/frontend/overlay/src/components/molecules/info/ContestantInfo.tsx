import React from "react";
import styled from "styled-components";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { RankLabel } from "../../atoms/ContestLabels";
import { formatScore, useFormatPenalty } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";
import { OptimismLevel, TeamId } from "@shared/api";
import { isShouldUseDarkColor } from "@/utils/colors";
import { getAwardsEffects } from "@/utils/awards";

const ContestantInfoLabel = styled(RankLabel).attrs({
    fontSize: c.CONTESTER_FONT_SIZE,
})`
    flex-shrink: 0;
    align-self: stretch;
    width: ${c.CONTESTER_INFO_RANK_WIDTH};
    padding-left: ${c.CONTESTER_INFO_LEFT_PADDING};
`;

const ContestantInfoTeamNameLabel = styled(ShrinkingBox).attrs({
    fontSize: c.CONTESTER_FONT_SIZE,
})`
    flex-grow: 1;
    width: ${c.CONTESTER_NAME_WIDTH};
`;

const ContestantInfoWrap = styled.div<{
    round: boolean;
    bg_color: string;
    color: string;
}>`
    overflow: hidden;
    display: flex;
    gap: ${c.CONTESTER_INFO_GAP};
    align-items: center;

    width: 100%;
    height: ${c.CONTESTER_ROW_HEIGHT};

    font-size: ${c.CONTESTER_FONT_SIZE};
    color: ${(props) => props.color};

    background-color: ${(props) => props.bg_color};
    border-radius: ${c.GLOBAL_BORDER_RADIUS}
        ${(props) => (props.round ? c.GLOBAL_BORDER_RADIUS : "0px")}
        ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS};
`;

const ContestantInfoScoreLabel = styled(ShrinkingBox).attrs({
    fontSize: c.CONTESTER_FONT_SIZE,
    align: "right",
})`
    flex-shrink: 0;
    box-sizing: content-box;
    width: ${c.CONTESTER_INFO_SCORE_WIDTH};
    padding-right: ${c.CONTESTER_INFO_SCORE_RIGHT_PADDING};
`;

export const ContestantInfo: React.FC<{
    teamId: TeamId;
    roundBR?: boolean;
    className?: string;
    useBG?: boolean;
}> = ({ teamId, roundBR = true, className = null, useBG = true }) => {
    const contestInfo = useAppSelector((state) => state.contestInfo.info);
    const scoreboardData = useAppSelector(
        (state) => state.scoreboard[OptimismLevel.normal].ids[teamId],
    );
    const awards = useAppSelector(
        (state) => state.scoreboard[OptimismLevel.normal].idAwards[teamId],
    );
    const rank = useAppSelector(
        (state) => state.scoreboard[OptimismLevel.normal].rankById[teamId],
    );
    const teamData = useAppSelector(
        (state) => state.contestInfo.info?.teamsId[teamId],
    );
    const formatPenalty = useFormatPenalty();
    const darkText = isShouldUseDarkColor(
        teamData?.color && useBG
            ? teamData?.color
            : c.CONTESTER_BACKGROUND_COLOR,
    );

    return (
        <ContestantInfoWrap
            round={roundBR}
            className={className}
            bg_color={
                teamData?.color && useBG
                    ? teamData?.color
                    : c.CONTESTER_BACKGROUND_COLOR
            }
            color={darkText ? "#000" : "#FFF"}
        >
            <ContestantInfoLabel
                rank={rank}
                effects={getAwardsEffects(awards)}
                bg_color={teamData?.color}
            />
            <ContestantInfoTeamNameLabel text={teamData?.shortName ?? "??"} />
            <ContestantInfoScoreLabel
                text={
                    scoreboardData === null
                        ? "??"
                        : formatScore(scoreboardData?.totalScore ?? 0.0, 1)
                }
            />
            {contestInfo?.resultType !== "IOI" && (
                <ContestantInfoScoreLabel
                    text={formatPenalty(scoreboardData?.penalty)}
                />
            )}
        </ContestantInfoWrap>
    );
};
