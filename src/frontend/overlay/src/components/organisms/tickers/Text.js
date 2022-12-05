import React from "react";
import styled from "styled-components";
import { TICKER_FONT_FAMILY, TICKER_TEXT_FONT_SIZE, TICKER_TEXT_MARGIN_LEFT } from "../../../config";
import { TextShrinkingCell } from "../../atoms/ContestCells";

export const TextWrap = styled.div`
    width: 100%;
    block-size: fit-content;
    margin-left: ${props => props.part === "long" ? TICKER_TEXT_MARGIN_LEFT : undefined};
    font-size: ${TICKER_TEXT_FONT_SIZE};
    display: flex;
    justify-content: ${props => props.part === "long" ? "flex-start" : "center"};
`;

export const Text = ({ tickerSettings, part }) => {
    return <TextWrap part={part}>
        <TextShrinkingCell
            text={tickerSettings.text}
            background={""}
            font={TICKER_TEXT_FONT_SIZE + " " + TICKER_FONT_FAMILY}
            align={"center"}/>
    </TextWrap>;
};

export default Text;
