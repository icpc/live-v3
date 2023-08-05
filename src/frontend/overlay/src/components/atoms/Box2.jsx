import PropTypes from "prop-types";
import styled from "styled-components";
import {
    CELL_TEXT_COLOR,
    GLOBAL_DEFAULT_FONT_FAMILY,
    GLOBAL_DEFAULT_FONT_SIZE,
} from "../../config";
import React, { useCallback, useEffect, useRef } from "react";
import { getTextWidth } from "./ContestCells";

export const Box2 = styled.div`
  // width: ${({ width }) => width};
  // height: ${({ height }) => height};

  // font-family: ${GLOBAL_DEFAULT_FONT_FAMILY};
  //font-size: ${GLOBAL_DEFAULT_FONT_SIZE};
  //color: ${CELL_TEXT_COLOR};

  //box-sizing: border-box;
  //overflow-x: hidden;
  //white-space: nowrap;
`;

/*
  font-weight: ${({ fontWeight }) => fontWeight};
  text-align: ${({ align }) => align};

  display: ${({ display }) => display};

  margin-left: ${({ marginLeft }) => marginLeft};
  margin-right: ${({ marginRight }) => marginRight};
  margin-top: ${({ marginTop }) => marginTop};
  margin-bottom: ${({ marginBottom }) => marginBottom};

  padding-top: ${({ paddingTop }) => paddingTop};

 */

Box2.propTypes = {
    width: PropTypes.string,
    height: PropTypes.string,
    background: PropTypes.string,
    fontSize: PropTypes.string,
    fontWeight: PropTypes.string,
    color: PropTypes.string,
    inverseColor: PropTypes.bool,
    align: PropTypes.string,
    display: PropTypes.string,
    marginLeft: PropTypes.string,
    marginRight: PropTypes.string,
    marginTop: PropTypes.string,
    marginBottom: PropTypes.string,
    paddingTop: PropTypes.string,
};

export const FlexedBox2 = styled(Box2)`
  display: flex;
  flex-grow: ${({ flexGrow }) => flexGrow ?? 0};
  flex-shrink: ${({ flexShrink }) => flexShrink ?? 0};
  flex-basis: ${({ flexBasis }) => flexBasis};
  align-items: center;
  justify-content: ${({ justifyContent }) => justifyContent ?? "center"};
`;

FlexedBox2.propTypes = {
    ...Box2.propTypes,
    flexGrow: PropTypes.number,
    flexShrink: PropTypes.number,
    flexBasis: PropTypes.number,
    justifyContent: PropTypes.string,
};

const TextShrinkingWrap = styled.div`
  display: flex;
  overflow: hidden;
  justify-content: ${props => props.align};
`;

export const ShrinkingBox2 = ({
    text,
    // children,
    fontFamily = GLOBAL_DEFAULT_FONT_FAMILY,
    fontSize = GLOBAL_DEFAULT_FONT_SIZE,
    align = "left",
    className
}) => {
    const boxRef = useRef(null);
    const updateScale = useCallback((newCellRef) => {
        if (newCellRef !== null) {
            boxRef.current = newCellRef;
            newCellRef.children[0].style.transform = "";
            const styles = getComputedStyle(newCellRef);
            const textWidth = getTextWidth(text, `${styles.fontWeight} ${styles.fontSize} ${styles.fontFamily}`);
            const haveWidth = (parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight)));
            const scaleFactor = Math.min(1, haveWidth / textWidth);
            newCellRef.children[0].style.transform = `scaleX(${scaleFactor})`;
        }
    }, [align, fontFamily, fontSize, text]);
    useEffect(() => {
        updateScale(boxRef.current);
    }, [text]);
    // console.log(props);
    return <TextShrinkingWrap ref={updateScale} align={align} className={className}>
        <TextShrinkingContainer2 align={align}>
            {text}
        </TextShrinkingContainer2>
    </TextShrinkingWrap>;
};

const TextShrinkingContainer2 = styled.div`
  transform-origin: ${({align}) => align};
  position: relative;
  text-align: ${({ align }) => align};
  color: ${({ color }) => color};
  white-space: nowrap;
`;
