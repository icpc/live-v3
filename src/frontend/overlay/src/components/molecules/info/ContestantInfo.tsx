import React from "react";
import styled from "styled-components";
import { SCOREBOARD_TYPES } from "@/consts";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { RankLabel } from "../../atoms/ContestLabels";
import { formatScore, useFormatPenalty } from "@/services/displayUtils";
import { useAppSelector } from "@/redux/hooks";


const ContestantInfoLabel = styled(RankLabel)`
  flex-shrink: 0;
  align-self: stretch;
  width: 32px;
  padding-left: 4px;
`;

const ContestantInfoTeamNameLabel = styled(ShrinkingBox)`
  flex-grow: 1;
  width: ${c.CONTESTER_NAME_WIDTH};
`;


const ContestantInfoWrap = styled.div<{round: boolean}>`
  overflow: hidden;
  display: flex;
  gap: 5px;
  align-items: center;

  width: 100%;
  height: ${c.CONTESTER_ROW_HEIGHT};

  font-size: ${c.CONTESTER_FONT_SIZE};
  color: white;

  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
  border-radius: ${c.GLOBAL_BORDER_RADIUS} ${props => props.round ? c.GLOBAL_BORDER_RADIUS : "0px"} ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS};
`;

const ContestantInfoScoreLabel = styled(ShrinkingBox)`
  flex-shrink: 0;
  box-sizing: content-box;
  width: 51px;
  padding-right: 20px;
`;


export const ContestantInfo = ({ teamId, roundBR= true, className = null }) => {
    const contestInfo = useAppSelector((state) => state.contestInfo.info);
    const scoreboardData = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[teamId]);
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const formatPenalty = useFormatPenalty();
    return <ContestantInfoWrap round={roundBR} className={className}>
        <ContestantInfoLabel rank={scoreboardData?.rank} medal={scoreboardData?.medalType}/>
        <ContestantInfoTeamNameLabel text={teamData?.shortName ?? "??"}/>
        <ContestantInfoScoreLabel align={"right"}
            text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}/>
        {contestInfo?.resultType !== "IOI" && <ContestantInfoScoreLabel align={"right"}
            text={formatPenalty(scoreboardData?.penalty)}/>}
    </ContestantInfoWrap>;
};
