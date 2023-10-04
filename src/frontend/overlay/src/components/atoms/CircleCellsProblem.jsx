import React from "react";
import styled from "styled-components";
import c from "../../config";
import { isShouldUseDarkColor } from "../../utils/colors";

const CircleCellProblemWrap = styled.div`
    position: relative;
    width: ${props => props.radius ?? c.CIRCLE_PROBLEM_SIZE};
    height: ${props => props.radius ?? c.CIRCLE_PROBLEM_SIZE};
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0;
    border-radius: 50%;
    font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
    border: ${props => props.borderColor ? `${props.borderColor} ${c.CIRCLE_PROBLEM_LINE_WIDTH} solid` : 0};
    background: ${props => props.backgroundColor};
`;

export const CircleCell = ({ content, borderColor, backgroundColor }) => {
    return <CircleCellProblemWrap
        borderColor={borderColor}
        backgroundColor={backgroundColor}
        inverseColor={isShouldUseDarkColor(backgroundColor)}>
        {content}
    </CircleCellProblemWrap>;
};
