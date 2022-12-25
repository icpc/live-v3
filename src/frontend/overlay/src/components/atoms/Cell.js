import PropTypes from "prop-types";
import styled, { keyframes } from "styled-components";
import {
    CELL_BG_COLOR,
    CELL_BG_COLOR_ODD,
    CELL_FLASH_PERIOD,
    CELL_FONT_FAMILY,
    CELL_FONT_SIZE,
    CELL_TEXT_COLOR
} from "../../config";

const flash = keyframes`
  from {
    filter: brightness(0.3);
  }
  to {
    filter: brightness(1);
  }
`;

// FIXME: too overloaded with props.
export const Cell = styled.div`
  width: ${props => props.width};
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: ${props => (props.canShrink ?? false) ? 1 : 0};
  flex-grow: ${props => (props.canGrow ?? false) ? 1 : 0};
  flex-basis: ${props => props.basis};

  font-family: ${CELL_FONT_FAMILY};
  font-size: ${CELL_FONT_SIZE};

  box-sizing: border-box;

  color: ${CELL_TEXT_COLOR};
  background-color: ${(props) => props.background ?? ((props.isEven && CELL_BG_COLOR_ODD) || CELL_BG_COLOR)};

  animation: ${props => props.flash ? flash : null} ${CELL_FLASH_PERIOD}ms linear infinite alternate-reverse;
`;

Cell.propTypes = {
    width: PropTypes.string,
    background: PropTypes.string,
    flex: PropTypes.string
};
