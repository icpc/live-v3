import React from "react";
import styled from "styled-components";
import { isShouldUseDarkColor } from "../../utils/colors";
import c from "../../config";

const StyledProblemLabel = styled.div<{
    backgroundColor: string;
    darkText: boolean;
}>`
  position: relative;

  display: flex;
  align-items: center;
  justify-content: center;

  width: ${c.PROBLEM_LABEL_WIDTH};
  height: 100%;

  color: ${({ darkText }) => darkText ? "#000" : "#FFF"};

  background: ${props => props.backgroundColor};
`;

interface ProblemLabelProps {
    letter?: string;
    problemColor?: string;
    className?: string;
}

export const ProblemLabel: React.FC<ProblemLabelProps> = ({ letter, problemColor, className }) => {
    const dark = isShouldUseDarkColor(problemColor);
    return <StyledProblemLabel
        backgroundColor={problemColor}
        darkText={dark}
        className={className}>
        {letter}
    </StyledProblemLabel>;
};