import PropTypes from "prop-types";
import styled, { keyframes } from "styled-components";
import {
    CELL_BG_COLOR2,
    CELL_BG_COLOR_ODD,
    CELL_FLASH_PERIOD,
    CONTESTER_ROW_BORDER_RADIUS,
    CONTESTER_ROW_OPACITY,
    MEDAL_COLORS,
    QUEUE_ROW_HEIGHT2,
} from "../../config";

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

export const ContestantRow2 = styled.div`
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

ContestantRow2.propTypes = {
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
