import React from "react";
import styled from "styled-components";
import c from "../../../config";
import { ShrinkingBox } from "../../atoms/ShrinkingBox";

export const TextWrap = styled.div`
    width: 100%;
    block-size: fit-content;
    font-size: ${c.TICKER_TEXT_FONT_SIZE};
    padding: 0 16px;
  box-sizing: border-box;
    display: flex;
    justify-content: ${props => props.part === "long" ? "flex-start" : "center"};
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
