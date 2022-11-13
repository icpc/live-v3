import PropTypes from "prop-types";
import React, { useCallback, useEffect, useRef } from "react";
import styled from "styled-components";
import {
    CELL_NAME_LEFT_PADDING,
    CELL_NAME_RIGHT_PADDING,
    CELL_PROBLEM_LINE_WIDTH,
    GLOBAL_DEFAULT_FONT,
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

const VerdictCellICPC = ({ data, props }) => {
    return <VerdictCellWrap
        background=
            {data.isJudged ?
                data.isAccepted ? VERDICT_OK : VERDICT_NOK
                : undefined}
        {...props}
    >
        {data.isFirstToSolve || data.isFirstSolvedRun && <StarIcon/>}
        {data.percentage !== 0 && !data.isJudged && <VerdictCellProgressBar width={data.percentage * 100 + "%"}/>}
        {data.isJudged && data.result}
    </VerdictCellWrap>;
};

VerdictCellICPC.PropTypes = {
    data: PropTypes.object
};

const VerdictCellScore = ({ data, props }) => {
    return <VerdictCellWrap
        background=
            {data.isJudged ?
                data.score > 0 ? VERDICT_OK : VERDICT_NOK
                : undefined}
        {...props}
    >
        {data.isFirstToSolve || data.isFirstSolvedRun && <StarIcon/>}
        {data.percentage !== 0 && !data.isJudged && <VerdictCellProgressBar width={data.percentage * 100 + "%"}/>}
        {data.isJudged && data.score}
    </VerdictCellWrap>;
};

VerdictCellScore.PropTypes = {
    data: PropTypes.object
};

export const VerdictCell = ({
    verdict: data,
    ...props
}) => {
    if(data.resultType === "ICPC") {
        return <VerdictCellICPC data={data} props={props} />;
    } else {
        return <VerdictCellScore data={data} props={props} />;
    }
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


const TextShrinkingContainer = styled.div`
  white-space: nowrap;
  transform-origin: left;
  text-align: left;
  position: relative;
  left: ${props => props.align === "center" ? "50%" : ""};
`;

const TextShrinkingWrap = styled(Cell)`
  flex-grow: ${props => (props.canGrow ?? true) ? 1 : 0};
  flex-shrink: ${props => (props.canShrink ?? true) ? 1 : 0};
  overflow-x: hidden;
  justify-content: start;
  padding-left: ${CELL_NAME_LEFT_PADDING};
  padding-right: ${CELL_NAME_RIGHT_PADDING};

  font: ${props => props.font};
`;

export const TextShrinkingCell = ({ text, font = GLOBAL_DEFAULT_FONT, align = "left", ...props }) => {
    const teamNameWidth = getTextWidth(text, font);
    const cellRef = useRef(null);
    const updateScale = useCallback((newCellRef) => {
        if (newCellRef !== null) {
            cellRef.current = newCellRef;
            newCellRef.children[0].style.transform = "";
            const styles = getComputedStyle(newCellRef);
            const haveWidth = parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight));
            const scaleFactor = Math.min(1, haveWidth / teamNameWidth);
            newCellRef.children[0].style.transform = `scaleX(${scaleFactor})${align === "center" ? " translateX(-50%)" : ""}`; // dirty hack, don't @ me
        }
    }, [align, font, text]);
    useEffect(() => {
        updateScale(cellRef.current);
        // console.log(cellRef.current);
    }, [text]);
    return <TextShrinkingWrap ref={updateScale} font={font} {...props}>
        <TextShrinkingContainer scaleY={0} align={align}>
            {text}
        </TextShrinkingContainer>
    </TextShrinkingWrap>;
};

TextShrinkingCell.propTypes = {
    ...Cell.propTypes,
    canGrow: PropTypes.bool,
    canShrink: PropTypes.bool,
    text: PropTypes.string.isRequired,
    align: PropTypes.oneOf(["center", "left"])
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
