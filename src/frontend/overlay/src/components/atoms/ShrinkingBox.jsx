import styled from "styled-components";
import c from "../../config";
import React, { useEffect, useRef, useMemo } from "react";

const TextShrinkingWrap = styled.div`
    overflow: hidden;
    display: flex;
    justify-content: ${(props) => props.align};

    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    font-kerning: none;

    /* Performance: isolate layout calculations */
    contain: layout style;
`;

const storage = window.localStorage;

let sharedCanvas = null;

export const getTextWidth = (text, font) => {
    const stringText = text + "";
    const key = stringText + ";" + font;
    const fontChecker = document.fonts && document.fonts.check(font);
    const cached = storage.getItem(key);

    if (cached) {
        return parseFloat(cached);
    }

    if (!sharedCanvas) {
        sharedCanvas = document.createElement("canvas");
    }

    const context = sharedCanvas.getContext("2d");
    context.font = font;
    context.fontKerning = "none";
    const metrics = context.measureText(stringText);
    const result = metrics.width;

    if (fontChecker) {
        storage.setItem(key, result.toString());
    }

    return result;
};

export const ShrinkingBox = React.memo(
    ({
        text,
        fontFamily = c.GLOBAL_DEFAULT_FONT_FAMILY,
        fontSize = c.GLOBAL_DEFAULT_FONT_SIZE,
        align = "left",
        className,
    }) => {
        const boxRef = useRef(null);
        const containerRef = useRef(null);
        const widthRef = useRef(0);
        const textWidthRef = useRef(0);

        const font = useMemo(() => {
            const weight = c.GLOBAL_DEFAULT_FONT_WEIGHT || "normal";
            return `${weight} ${fontSize} ${fontFamily}`;
        }, [fontFamily, fontSize]);

        const textWidth = useMemo(() => {
            return getTextWidth(text, font);
        }, [text, font]);

        useEffect(() => {
            textWidthRef.current = textWidth;
        }, [textWidth]);

        const scaleFactor = 1;

        useEffect(() => {
            const cellRef = boxRef.current;
            const contentRef = containerRef.current;
            if (!cellRef || !contentRef) return;

            const updateTransform = (width) => {
                if (
                    width === widthRef.current &&
                    textWidthRef.current === textWidth
                )
                    return;
                widthRef.current = width;
                const newScaleFactor = Math.min(
                    1,
                    width / textWidthRef.current,
                );
                contentRef.style.transform = `scaleX(${newScaleFactor})`;
            };

            updateTransform(cellRef.offsetWidth);

            const observer = new ResizeObserver((entries) => {
                requestAnimationFrame(() => {
                    for (const entry of entries) {
                        if (entry.target === cellRef) {
                            updateTransform(entry.contentRect.width);
                        }
                    }
                });
            });

            observer.observe(cellRef);

            return () => {
                observer.disconnect();
            };
        }, [textWidth]);

        return (
            <TextShrinkingWrap ref={boxRef} align={align} className={className}>
                <TextShrinkingContainer
                    ref={containerRef}
                    align={align}
                    scaleFactor={scaleFactor}
                >
                    {text}
                </TextShrinkingContainer>
            </TextShrinkingWrap>
        );
    },
    (prevProps, nextProps) => {
        return (
            prevProps.text === nextProps.text &&
            prevProps.fontFamily === nextProps.fontFamily &&
            prevProps.fontSize === nextProps.fontSize &&
            prevProps.align === nextProps.align &&
            prevProps.className === nextProps.className
        );
    },
);

const TextShrinkingContainer = styled.div`
    position: relative;
    transform-origin: ${({ align }) => align};

    color: inherit;
    text-align: ${({ align }) => align};
    white-space: nowrap;

    /* Use transform for GPU acceleration */
    transform: scaleX(${({ scaleFactor }) => scaleFactor});
    will-change: transform;
`;
