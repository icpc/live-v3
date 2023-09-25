import PropTypes from "prop-types";
import React from "react";
import { useSelector } from "react-redux";
import styled, { keyframes } from "styled-components";
import { SCOREBOARD_TYPES } from "../../../consts";
import {
    CELL_BG_COLOR2,
    CELL_BG_COLOR_ODD,
    CELL_FLASH_PERIOD, CONTESTER_BACKGROUND_COLOR, CONTESTER_NAME_WIDTH,
    CONTESTER_ROW_BORDER_RADIUS, CONTESTER_ROW_HEIGHT,
    CONTESTER_ROW_OPACITY,
    QUEUE_ROW_HEIGHT2
} from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import {RankLabel} from "../../atoms/ContestLabels";
import { formatScore } from "../../atoms/ContestCells";


const rowFlashing = keyframes`
  from {
    filter: brightness(0.3);
  }
  to {
    filter: brightness(1);
  }
`;

const getContesterRowBackground = (background, medal, isEven) => {
    const base = background ?? ((isEven && CELL_BG_COLOR_ODD) || CELL_BG_COLOR2);
    if (MEDAL_COLORS[medal]) {
        return `linear-gradient(270deg, rgba(253, 141, 105, 0) 0, ${MEDAL_COLORS[medal]} 100%)` + base;
    }
    return base;
};

const borderRadius = ({
    round,
    roundT,
    roundB,
    roundTL,
    roundTR,
    roundBL,
    roundBR,
}) => {
    const borderRadiusTL = (roundTL ?? roundT ?? round ?? true) ? CONTESTER_ROW_BORDER_RADIUS : 0;
    const borderRadiusTR = (roundTR ?? roundT ?? round ?? true) ? CONTESTER_ROW_BORDER_RADIUS : 0;
    const borderRadiusBL = (roundBL ?? roundB ?? round ?? true) ? CONTESTER_ROW_BORDER_RADIUS : 0;
    const borderRadiusBR = (roundBR ?? roundB ?? round ?? true) ? CONTESTER_ROW_BORDER_RADIUS : 0;
    return `${borderRadiusTL} ${borderRadiusTR} ${borderRadiusBR} ${borderRadiusBL}`;
};

export const ContestantRow = styled.div`
  background-color: ${({ background, medal, isEven }) => getContesterRowBackground(background, medal, isEven)};
  background-size: 34px 100%; /* TODO: 34 is a magic number for gradient medal color */
  background-repeat: no-repeat;
  border-radius: ${props => borderRadius(props)};

  height: ${QUEUE_ROW_HEIGHT2}px;
  display: flex;
  flex-wrap: nowrap;
  max-width: 100%;
  opacity: ${CONTESTER_ROW_OPACITY};
  padding: 0 10px;

  animation: ${props => props.flashing ? rowFlashing : null} ${CELL_FLASH_PERIOD}ms linear infinite alternate-reverse;
`;

ContestantRow.propTypes = {
    background: PropTypes.string,
    medal: PropTypes.string,
    isEven: PropTypes.bool,
    flashing: PropTypes.bool,
    round : PropTypes.bool,
    roundT : PropTypes.bool,
    roundB : PropTypes.bool,
    roundTL : PropTypes.bool,
    roundTR : PropTypes.bool,
    roundBL : PropTypes.bool,
    roundBR : PropTypes.bool,
};

const ContestantInfoLabel = styled(RankLabel)`
  width: 32px;
  align-self: stretch;
  padding-left: 4px;
  flex-shrink: 0;
`;

const ContestantInfoTeamNameLabel = styled(ShrinkingBox)`
  flex-grow: 1;
  width: ${CONTESTER_NAME_WIDTH};
  //flex-shrink: 0;
`;


const ContestantInfoWrap = styled.div`
  width: 100%;
  height: ${CONTESTER_ROW_HEIGHT};
  background-color: ${CONTESTER_BACKGROUND_COLOR};
  display: flex;
  align-items: center;
  border-radius: 16px ${props => props.round ? "16px" : "0px"} 16px  16px ;
  overflow: hidden;
  gap: 5px;
  color: white;
  font-size: 18px;
`;

const ContestantInfoScoreLabel = styled(ShrinkingBox)`
  width: 51px;
  flex-shrink: 0;
  padding-right: 20px;
  flex-direction: row-reverse;
`;


export const ContestantInfo = ({ teamId, roundBR }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].ids[teamId]);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);

    return <ContestantInfoWrap round={roundBR}>
        <ContestantInfoLabel rank={scoreboardData?.rank} medal={scoreboardData?.medalType}/>
        <ContestantInfoTeamNameLabel text={teamData?.shortName ?? "??"}/>
        <ContestantInfoScoreLabel align={"right"}
                         text={scoreboardData === null ? "??" : formatScore(scoreboardData?.totalScore ?? 0.0, 1)}/>
        {contestInfo?.resultType !== "IOI" && <ContestantInfoScoreLabel align={"right"}
                                  text={scoreboardData === null ? "??" : formatScore(scoreboardData?.penalty ?? 0.0, 1)}/>}


    </ContestantInfoWrap>;
};
