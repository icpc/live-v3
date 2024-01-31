import styled from "styled-components";
import c from "../../config";
import React, { memo, useCallback, useEffect, useRef } from "react";

const TextShrinkingWrap = styled.div`
  overflow: hidden;
  display: flex;
  justify-content: ${props => props.align};

  font-family: Arial, sans-serif;
  font-kerning: none; /* Remove after https://bugs.chromium.org/p/chromium/issues/detail?id=1192834 is fixed. */
`;

const storage = window.localStorage;
export const getTextWidth = (text, font) => {
    const stringText = text + "";
    const key = stringText + ";" + font;
    const cached = storage.getItem(key);
    if (cached) {
        return cached;
    } else {
        // re-use canvas object for better performance
        let canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"));
        const context = canvas.getContext("2d");
        context.font = font;
        context.fontKerning = "none"; // Remove after https://bugs.chromium.org/p/chromium/issues/detail?id=1192834 is fixed.
        const metrics = context.measureText(stringText);
        const result = metrics.width;
        storage.setItem(key, result);
        return result;
    }
};
export const ShrinkingBox = memo(({
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
            const haveWidth = (parseFloat(styles.width));
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
});

const TextShrinkingContainer = styled.div`
  position: relative;
  transform-origin: ${({ align }) => align};

  color: ${({ color }) => color};
  text-align: ${({ align }) => align};
  white-space: nowrap;
`;
