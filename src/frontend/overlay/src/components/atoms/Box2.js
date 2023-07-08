import PropTypes from "prop-types";
import styled from "styled-components";
import {
    CELL_TEXT_COLOR,
    CELL_TEXT_COLOR_INVERSE,
    GLOBAL_DEFAULT_FONT,
    GLOBAL_DEFAULT_FONT_FAMILY,
    GLOBAL_DEFAULT_FONT_SIZE,
} from "../../config";
import React, { useCallback, useEffect, useRef } from "react";
import { getTextWidth } from "./ContestCells";

export const Box2 = styled.div`
  width: ${({ width }) => width};
  height: ${({ height }) => height ?? "100%"};
  
  font-family: ${GLOBAL_DEFAULT_FONT_FAMILY};
  font-size: ${({ fontSize }) => fontSize ?? GLOBAL_DEFAULT_FONT_SIZE};
  font-weight: ${({ fontWeight }) => fontWeight};
  color: ${({ color, inverseColor }) => color ?? (inverseColor ? CELL_TEXT_COLOR_INVERSE : CELL_TEXT_COLOR)};
  text-align: ${({ align }) => align};
  
  display: ${({ display }) => display};

  margin-left: ${({ marginLeft }) => marginLeft};
  margin-right: ${({ marginRight }) => marginRight};
  margin-top: ${({ marginTop }) => marginTop};
  margin-bottom: ${({ marginBottom }) => marginBottom};

  padding-top: ${({ paddingTop }) => paddingTop};

  box-sizing: border-box;
  overflow-x: hidden;
  white-space: nowrap;
`;

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
  flex-grow: ${({ flexGrow }) => flexGrow ?? 0};
  flex-shrink: ${({ flexShrink }) => flexShrink ?? 0};
  flex-basis: ${({ flexBasis }) => flexBasis};
  align-items: center;
  justify-content: ${({ justifyContent }) => justifyContent ?? "center"};
  flex-basis: ${props => props.basis};
`;

FlexedBox2.propTypes = {
    ...Box2.propTypes,
    flexGrow: PropTypes.number,
    flexShrink: PropTypes.number,
    flexBasis: PropTypes.number,
    justifyContent: PropTypes.string,
};

export const ShrinkingBox2 = ({
    text,
    // children,
    fontFamily = GLOBAL_DEFAULT_FONT_FAMILY,
    fontSize = GLOBAL_DEFAULT_FONT_SIZE,
    align = "left",
    color,
    Wrapper = Box2,
    ...props
}) => {
    const boxRef = useRef(null);
    const updateScale = useCallback((newCellRef) => {
        if (newCellRef !== null) {
            boxRef.current = newCellRef;
            newCellRef.children[0].style.transform = "";
            const styles = getComputedStyle(newCellRef);
            const textWidth = getTextWidth(text, `${fontSize} ${fontFamily}`);
            const haveWidth = (parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight)));
            const scaleFactor = Math.min(1, haveWidth / textWidth);
            newCellRef.children[0].style.transform = `scaleX(${scaleFactor})`;
        }
    }, [align, fontFamily, fontSize, text]);
    useEffect(() => {
        updateScale(boxRef.current);
    }, [text]);
    return <Wrapper ref={updateScale} {...props}>
        <TextShrinkingContainer2 align={align} color={color} fontSize={fontSize} fontFamily={fontFamily}>
            {text}
        </TextShrinkingContainer2>
    </Wrapper>;
};


const TextShrinkingContainer2 = styled.div`
  transform-origin: left;
  position: relative;
  text-align: ${({ align }) => align};
  color: ${({ color }) => color};
  font-family: ${({ fontFamily }) => fontFamily};
  font-size: ${({ fontSize }) => fontSize};
`;

export const TextShrinking2 = ({ text, font = GLOBAL_DEFAULT_FONT, align = "left", children, ...props }) => {
    const textWidth = getTextWidth(text, font);
    const cellRef = useRef(null);
    const updateScale = useCallback((newCellRef) => {
        if (newCellRef !== null) {
            cellRef.current = newCellRef;
            newCellRef.children[0].style.transform = "";
            const styles = getComputedStyle(newCellRef);
            const haveWidth = (parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight)));
            const scaleFactor = Math.min(1, haveWidth / textWidth);
            newCellRef.children[0].style.transform = `scaleX(${scaleFactor})${align === "center" ? " translateX(-50%)" : ""}`; // dirty hack, don't @ me
        }
    }, [align, font, text]);
    useEffect(() => {
        updateScale(cellRef.current);
    }, [text]);
    return <Box2 ref={updateScale} {...props}>
        <TextShrinkingContainer2 scaleY={0} align={align}>
            {text}
        </TextShrinkingContainer2>
        {children}
    </Box2>;
};
