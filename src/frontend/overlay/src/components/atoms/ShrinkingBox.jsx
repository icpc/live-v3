import styled from "styled-components";
import c from "../../config";
import React, { useCallback, useEffect, useRef } from "react";
import { getTextWidth } from "./ContestCells";

const TextShrinkingWrap = styled.div`
  display: flex;
  overflow: hidden;
  justify-content: ${props => props.align};
`;

export const ShrinkingBox = ({
    text,
    fontFamily = c.GLOBAL_DEFAULT_FONT_FAMILY,
    fontSize = c.GLOBAL_DEFAULT_FONT_SIZE,
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
    return <TextShrinkingWrap ref={updateScale} align={align} className={className}>
        <TextShrinkingContainer align={align}>
            {text}
        </TextShrinkingContainer>
    </TextShrinkingWrap>;
};

const TextShrinkingContainer = styled.div`
  transform-origin: ${({ align }) => align};
  position: relative;
  text-align: ${({ align }) => align};
  color: ${({ color }) => color};
  white-space: nowrap;
`;
