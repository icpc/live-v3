import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { SCOREBOARD_TYPES } from "../../../consts";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { RankLabel } from "../../atoms/ContestLabels";
import { formatScore, useFormatPenalty } from "../../../services/displayUtils";


// const rowFlashing = keyframes`
//   from {
//     filter: brightness(0.3);
//   }
//   to {
//     filter: brightness(1);
//   }
// `;
// const borderRadius = ({
//     round,
//     roundT,
//     roundB,
//     roundTL,
//     roundTR,
//     roundBL,
//     roundBR,
// }) => {
//     const borderRadiusTL = (roundTL ?? roundT ?? round ?? true) ? c.CONTESTER_ROW_BORDER_RADIUS : 0;
//     const borderRadiusTR = (roundTR ?? roundT ?? round ?? true) ? c.CONTESTER_ROW_BORDER_RADIUS : 0;
//     const borderRadiusBL = (roundBL ?? roundB ?? round ?? true) ? c.CONTESTER_ROW_BORDER_RADIUS : 0;
//     const borderRadiusBR = (roundBR ?? roundB ?? round ?? true) ? c.CONTESTER_ROW_BORDER_RADIUS : 0;
//     return `${borderRadiusTL} ${borderRadiusTR} ${borderRadiusBR} ${borderRadiusBL}`;
// };
const ContestantInfoLabel = styled(RankLabel)`
  width: 32px;
  align-self: stretch;
  padding-left: 4px;
  flex-shrink: 0;
`;

const ContestantInfoTeamNameLabel = styled(ShrinkingBox)`
  flex-grow: 1;
  width: ${c.CONTESTER_NAME_WIDTH};
  //flex-shrink: 0;
`;


const ContestantInfoWrap = styled.div`
  width: 100%;
  height: ${c.CONTESTER_ROW_HEIGHT};
  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
  display: flex;
  align-items: center;
  border-radius: ${c.GLOBAL_BORDER_RADIUS} ${props => props.round ? c.GLOBAL_BORDER_RADIUS : "0px"} ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS};
  overflow: hidden;
  gap: 5px;
  color: white;
  font-size: ${c.CONTESTER_FONT_SIZE};
`;

const ContestantInfoScoreLabel = styled(ShrinkingBox)`
  width: 51px;
  flex-shrink: 0;
  padding-right: 20px;
  box-sizing: content-box;
`;


export const ContestantInfo = ({ teamId, roundBR= true, className }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
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
