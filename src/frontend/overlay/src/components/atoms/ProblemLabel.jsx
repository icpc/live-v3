import PropTypes from "prop-types";
import styled from "styled-components";
import { isShouldUseDarkColor } from "../../utils/colors";

const StyledProblemLabel = styled.div`
  position: relative;
  
  display: flex;
  align-items: center;
  justify-content: center;

  width: 28px;
  height: 100%;

  color: ${({ darkText }) => darkText ? "#000" : "#FFF"};

  background: ${props => props.backgroundColor};
`;

export const ProblemLabel = ({ letter, problemColor, className }) => {
    const dark = isShouldUseDarkColor(problemColor);
    // console.log(dark);
    return <StyledProblemLabel
        backgroundColor={problemColor}
        darkText={dark}
        className={className}>
        {letter}
    </StyledProblemLabel>;
};

ProblemLabel.propTypes = {
    letter: PropTypes.string,
    problemColor: PropTypes.string,
    className: PropTypes.string
};
