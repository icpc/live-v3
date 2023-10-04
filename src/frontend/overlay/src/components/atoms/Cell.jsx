import PropTypes from "prop-types";
import styled, { keyframes } from "styled-components";
import c from "../../config";

const flash = keyframes`
  from {
    filter: brightness(0.3);
  }
  to {
    filter: brightness(1);
  }
`;

// FIXME: too overloaded with props.
/**
 * @deprecated Do not use this component or inherit from this component. If you need styles - use them ad hoc.
 * @type {StyledComponent<"div", AnyIfEmpty<DefaultTheme>, {}, never>}
 */
export const Cell = styled.div`
  width: ${props => props.width};
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: ${props => (props.canShrink ?? false) ? 1 : 0};
  flex-grow: ${props => (props.canGrow ?? false) ? 1 : 0};
  flex-basis: ${props => props.basis};

  font-family: ${c.CELL_FONT_FAMILY};
  font-size: ${c.CELL_FONT_SIZE};

  box-sizing: border-box;

  color: ${c.CELL_TEXT_COLOR};
  background-color: ${(props) => props.background ?? ((props.isEven && c.CELL_BG_COLOR_ODD) || c.CELL_BG_COLOR)};

  animation: ${props => props.flash ? flash : null} ${c.CELL_FLASH_PERIOD}ms linear infinite alternate-reverse;
`;

Cell.propTypes = {
    width: PropTypes.string,
    background: PropTypes.string,
    flex: PropTypes.string
};
