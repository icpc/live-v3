import styled from "styled-components";
import c from "../../config";
import React, { useEffect, useRef } from "react";

const TextShrinkingWrap = styled.div`
  overflow: hidden;
  display: flex;
  justify-content: ${props => props.align};

  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  font-kerning: none; /* Remove after https://bugs.chromium.org/p/chromium/issues/detail?id=1192834 is fixed. */
`;

const storage = window.localStorage;
export const getTextWidth = (text, font) => {
    const stringText = text + "";
    const key = stringText + ";" + font;
    // TODO: Maybe delete this, need to test
    const fontChecker = document.fonts && document.fonts.check(font);
    // console.log(fontChecker);
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
        if (fontChecker) {
            storage.setItem(key, result);
        }
        return result;
    }
};
export const ShrinkingBox = ({
    text,
    fontFamily = c.GLOBAL_DEFAULT_FONT_FAMILY,
    fontSize = c.GLOBAL_DEFAULT_FONT_SIZE,
    align = "left",
    className
}) => {
    const boxRef = useRef(null);
    const observerRef = useRef(null);
    const updateScale = () => {
        const cellRef = boxRef.current;
        if (cellRef !== null) {
            cellRef.children[0].style.transform = "";
            const styles = getComputedStyle(cellRef);
            const font = `${styles.fontWeight} ${styles.fontSize} ${styles.fontFamily}`;
            const textWidth = getTextWidth(text, font);
            const haveWidth = (parseFloat(styles.width));
            const scaleFactor = Math.min(1, haveWidth / textWidth);
            // console.log(`Shrinking, ${text}, font=${font}, width=${textWidth}, have=${haveWidth}, scale=${scaleFactor} debug=${haveWidth / textWidth}`);
            cellRef.children[0].style.transform = `scaleX(${scaleFactor})`;
        }
    };
    useEffect(() => {
        updateScale();
    }, [text]);
    const bindObserver = (cellRef) => {
        boxRef.current = cellRef;
        if (cellRef !== null) {
            observerRef.current = new ResizeObserver((entries) => {
                for (const entry of entries) {
                    if (entry.target === cellRef) {
                        updateScale(cellRef);
                    }
                }
            });
            observerRef.current.observe(cellRef);
        }
    };
    useEffect(() => {
        return () => {
            if(observerRef.current !== null) {
                observerRef.current.disconnect();
            }
        };
    });
    return <TextShrinkingWrap ref={bindObserver} align={align} className={className}>
        <TextShrinkingContainer align={align}>
            {text}
        </TextShrinkingContainer>
    </TextShrinkingWrap>;
};

const TextShrinkingContainer = styled.div`
  position: relative;
  transform-origin: ${({ align }) => align};

  color: ${({ color }) => color};
  text-align: ${({ align }) => align};
  white-space: nowrap;
`;
