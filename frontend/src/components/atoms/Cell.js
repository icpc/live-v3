import PropTypes from "prop-types";
import styled from "styled-components";
import { CELL_BG_COLOR, CELL_FONT_FAMILY, CELL_FONT_SIZE, CELL_TEXT_COLOR } from "../../config";

export const Cell = styled.div`
  width: ${props => props.width};
  height: 100%;
  display: inline-block;
  flex-shrink: 0;
  flex: ${props => props.flex};
  flex-basis: ${props => props.width};

  text-align: center;
  font-family: ${CELL_FONT_FAMILY};
  font-size: ${CELL_FONT_SIZE};
  
  box-sizing: border-box;
  
  color: ${CELL_TEXT_COLOR};
  background-color: ${(props) => props.background || CELL_BG_COLOR};
`;

Cell.propTypes = {
    width: PropTypes.string,
    background: PropTypes.string,
    flex: PropTypes.string
};
