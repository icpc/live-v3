import React from "react";
import styled from "styled-components";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";
import { TickerPart } from "@shared/api";

export const TextWrap = styled.div<{part: TickerPart}>`
    display: flex;
    justify-content: ${props => props.part === "long" ? "flex-start" : "center"};

    box-sizing: border-box;
    width: 100%;
    block-size: fit-content;
    padding: 0 ${c.TICKER_TEXT_HORIZONTAL_PADDING};

    font-size: ${c.TICKER_TEXT_FONT_SIZE};
`;

export const Text = ({ tickerSettings, part }) => {
    return <TextWrap part={part}>
        <ShrinkingBox
            text={tickerSettings.text}
            fontSize={c.TICKER_TEXT_FONT_SIZE}
            fontFamily={c.TICKER_FONT_FAMILY}
            align={part === "long" ? "left" : "center"}
        />
    </TextWrap>;
};

export default Text;
