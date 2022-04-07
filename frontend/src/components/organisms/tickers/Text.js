import React from "react";
import styled from "styled-components";

export const TextWrap = styled.div`
    width: 100%;
    block-size: fit-content;
    margin-left: 7px;
    font-size: 34px;
`;

export const Text = ({ tickerSettings }) => {
    return <TextWrap>
        {tickerSettings.text}
    </TextWrap>;
};

export default Text;
