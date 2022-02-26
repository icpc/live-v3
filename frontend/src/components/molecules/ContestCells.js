import PropTypes from "prop-types";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_FONT_FAMILY,
    CELL_FONT_SIZE,
    CELL_PROBLEM_LINE_WIDTH,
    CELL_QUEUE_VERDICT_WIDTH,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../config";
import { Cell } from "../atoms/Cell";

export const ProblemCellWrap = styled(Cell) `
  border-bottom: ${props => props.probColor} ${CELL_PROBLEM_LINE_WIDTH} solid;
  flex-shrink: 0;
`;

export const ProblemCell = ({ probInd, ...props }) => {
    const probData = useSelector((state) => state.contestInfo.info?.problems[probInd]);
    return <ProblemCellWrap probColor={probData?.color ?? "red"} {...props}>
        {probData?.name ?? "??"}
    </ProblemCellWrap>;
};

ProblemCell.propTypes = {
    ...Cell.propTypes,
    probInd: PropTypes.number.isRequired
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


export const VerdictCell = ({ verdict: { isAccepted, isJudged, result, percentage }, ...props }) => {
    return <Cell
        background={isJudged ?
            isAccepted ? VERDICT_OK : VERDICT_NOK
            : undefined}
        width={CELL_QUEUE_VERDICT_WIDTH}
        {...props}
    >
        {percentage !== 0 && !isJudged && <VerdictCellProgressBar width={percentage*100+"%"}/>}
        {isJudged && result}
    </Cell>;
};

VerdictCell.propTypes = {
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
  height: 100%;
  white-space: nowrap;
  transform: scaleX(${props => props.scaleY});
  width: 1px;
`;

const TeamNameWrap = styled(Cell)`
  flex-grow: 1;
  flex-shrink: 1;
`;

export const TeamNameCell = ({ teamName }) => {
    const cellRef = useRef(null);
    const teamNameWidth = useMemo(() => getTextWidth(teamName, CELL_FONT_SIZE + " " + CELL_FONT_FAMILY), [teamName]);
    console.log(teamNameWidth);
    const [scaleFactor, setScaleFactor] = useState(undefined);
    useEffect(() => {
        if(cellRef.current !== null) {
            const styles = getComputedStyle(cellRef.current);
            const haveWidth = parseFloat(styles.width);
            console.log(teamName, teamNameWidth, haveWidth);
            setScaleFactor(Math.min(1, haveWidth/teamNameWidth));
        }
    }, [cellRef, teamNameWidth]);
    // console.log(scaleFactor);
    return <TeamNameWrap ref={cellRef}>
        {scaleFactor !== undefined &&
            <TeamNameCellContainer scaleY={scaleFactor}>
                {teamName}
            </TeamNameCellContainer>
        }
    </TeamNameWrap>;
};

TeamNameCell.propTypes = {
    teamName: PropTypes.string.isRequired
};
