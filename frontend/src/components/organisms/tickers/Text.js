import React from "react";
import styled from "styled-components";
import { TICKER_TEXT_FONT_SIZE, TICKER_TEXT_MARGIN_LEFT } from "../../../config";

export const TextWrap = styled.div`
    //width: 100%;
    block-size: fit-content;
    margin-left: ${TICKER_TEXT_MARGIN_LEFT};
    font-size: ${TICKER_TEXT_FONT_SIZE};
`;

export const Text = ({ tickerSettings }) => {
    return <TextWrap>
        {tickerSettings.text}
    </TextWrap>;
};

export default Text;
