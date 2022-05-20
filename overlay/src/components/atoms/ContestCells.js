import PropTypes from "prop-types";
import React, { useRef } from "react";
import styled from "styled-components";
import {
    CELL_NAME_FONT,
    CELL_NAME_LEFT_PADDING,
    CELL_NAME_RIGHT_PADDING,
    CELL_PROBLEM_LINE_WIDTH,
    MEDAL_COLORS,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../config";
import { Cell } from "./Cell";
import { StarIcon } from "./Star";

export const ProblemCellWrap = styled(Cell)`
  border-bottom: ${props => props.probColor} ${CELL_PROBLEM_LINE_WIDTH} solid;
`;

export const ProblemCell = ({ probData, ...props }) => {
    return <ProblemCellWrap probColor={probData?.color ?? "red"} {...props}>
        {probData?.letter ?? "??"}
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
  left: 0;
  background-color: ${VERDICT_UNKNOWN};
`;

const VerdictCellWrap = styled(Cell)`
  position: relative;
`;

export const VerdictCell = ({ verdict: { isAccepted, isJudged, result, percentage, isFirstToSolve, isFirstSolvedRun }, ...props }) => {
    return <VerdictCellWrap
        background=
            {isJudged ?
                isAccepted ? VERDICT_OK : VERDICT_NOK
                : undefined}
        {...props}
    >
        {isFirstToSolve || isFirstSolvedRun && <StarIcon/>}
        {percentage !== 0 && !isJudged && <VerdictCellProgressBar width={percentage * 100 + "%"}/>}
        {isJudged && result}
    </VerdictCellWrap>;
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

const storage = window.localStorage;
export const getTextWidth = (text, font) => {
    const key = text + ";" + font;
    const cached = storage.getItem(key);
    if (cached) {
        return cached;
    } else {
        // re-use canvas object for better performance
        const canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"));
        const context = canvas.getContext("2d");
        context.font = font;
        const metrics = context.measureText(text);
        const result = metrics.width;
        storage.setItem(key, result);
        return result;
    }
};


const TeamNameCellContainer = styled.div.attrs(({ scaleY }) => ({
    style: {
        transform: `scaleX(${scaleY})`
    }
}))`
  white-space: nowrap;
  transform-origin: left;
  text-align: left;
`;

const TeamNameWrap = styled(Cell)`
  flex-grow: ${props => (props.canGrow ?? true) ? 1 : 0};
  flex-shrink: ${props => (props.canShrink ?? true) ? 1 : 0};
  overflow-x: hidden;
  justify-content: start;
  padding-left: ${CELL_NAME_LEFT_PADDING};
  padding-right: ${CELL_NAME_RIGHT_PADDING};
`;

export const TeamNameCell = ({ teamName, ...props }) => {
    const cellRef = useRef(null);
    const teamNameWidth = getTextWidth(teamName, CELL_NAME_FONT);
    let scaleFactor = undefined;
    if (cellRef.current !== null) {
        const styles = getComputedStyle(cellRef.current);
        const haveWidth = parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight));
        scaleFactor = Math.min(1, haveWidth / teamNameWidth);
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

export const RankCell = ({ rank, medal, ...props }) => {
    return <Cell background={MEDAL_COLORS[medal]} {...props}>
        {rank ?? "??"}
    </Cell>;
};

RankCell.propTypes = {
    ...Cell.propTypes,
    rank: PropTypes.number
};
