import PropTypes from "prop-types";
import React, { useRef } from "react";
import styled from "styled-components";
import {
    CELL_FONT_FAMILY,
    CELL_FONT_SIZE,
    CELL_NAME_LEFT_PADDING,
    CELL_NAME_RIGHT_PADDING,
    CELL_PROBLEM_LINE_WIDTH,
    getMedalColor,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../config";
import { Cell } from "./Cell";
import { StarIcon } from "./Star";

export const ProblemCellWrap = styled(Cell) `
  border-bottom: ${props => props.probColor} ${CELL_PROBLEM_LINE_WIDTH} solid;
`;

export const ProblemCell = ({ probData, ...props }) => {
    return <ProblemCellWrap probColor={probData?.color ?? "red"} {...props}>
        {probData?.name ?? "??"}
    </ProblemCellWrap>;
};

ProblemCell.propTypes = {
    ...Cell.propTypes,
    probData: PropTypes.object
};

const VerdictCellProgressBar = styled.div.attrs(({ width }) => ({
    style: {
        width
    }
}))`
    height: 100%;
    position: absolute;
  background-color: ${VERDICT_UNKNOWN};
`;


export const VerdictCell = ({ verdict: { isAccepted, isJudged, result, percentage, isFirstToSolve }, ...props }) => {
    return <Cell
        background={isJudged ?
            isAccepted ? VERDICT_OK : VERDICT_NOK
            : undefined}
        {...props}
    >
        {isFirstToSolve && <StarIcon/>}
        {percentage !== 0 && !isJudged && <VerdictCellProgressBar width={percentage*100+"%"}/>}
        {isJudged && result}
    </Cell>;
};

VerdictCell.propTypes = {
    ...Cell.propTypes,
    verdict: PropTypes.shape({
        isAccepted: PropTypes.bool.isRequired,
        isJudged: PropTypes.bool.isRequired,
        result: PropTypes.string.isRequired,
        percentage: PropTypes.number.isRequired
    })
};

function getTextWidth(text, font) {
    // re-use canvas object for better performance
    const canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"));
    const context = canvas.getContext("2d");
    context.font = font;
    const metrics = context.measureText(text);
    return metrics.width;
}


const TeamNameCellContainer = styled.div`
  white-space: nowrap;
  transform: scaleX(${props => props.scaleY});
  transform-origin: left;
  text-align: left;
`;

const TeamNameWrap = styled(Cell)`
  flex-grow: ${props => (props.canGrow ?? true) ? 1 : 0};
  flex-shrink: ${props => (props.canShrink ?? true) ? 1 : 0};
  overflow-x: clip;
  justify-content: start;
  padding-left: ${CELL_NAME_LEFT_PADDING};
  padding-right: ${CELL_NAME_RIGHT_PADDING};
`;

export const TeamNameCell = ({ teamName, ...props }) => {
    const cellRef = useRef(null);
    const teamNameWidth = getTextWidth(teamName, CELL_FONT_SIZE + " " + CELL_FONT_FAMILY);
    let scaleFactor = undefined;
    if(cellRef.current !== null) {
        const styles = getComputedStyle(cellRef.current);
        const haveWidth = parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight));
        scaleFactor = Math.min(1, haveWidth/teamNameWidth);
    }
    return <TeamNameWrap ref={cellRef} {...props}>
        {scaleFactor !== undefined &&
            <TeamNameCellContainer scaleY={scaleFactor}>
                {teamName}
            </TeamNameCellContainer>
        }
    </TeamNameWrap>;
};

TeamNameCell.propTypes = {
    ...Cell.propTypes,
    canGrow: PropTypes.bool,
    canShrink: PropTypes.bool,
    teamName: PropTypes.string.isRequired
};

export const RankCell = ({ rank, ...props }) => {
    return <Cell background={getMedalColor(rank)} {...props}>
        {rank ?? "??"}
    </Cell>;
};

RankCell.propTypes = {
    ...Cell.propTypes,
    rank: PropTypes.number
};
