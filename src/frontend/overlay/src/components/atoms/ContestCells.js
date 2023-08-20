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
    VERDICT_UNKNOWN,
    SCORE_NONE_TEXT
} from "../../config";
import { Cell } from "./Cell";
import { StarIcon } from "./Star";

export const formatScore = (score, digits = 2) => {
    if (score === undefined) {
        return SCORE_NONE_TEXT;
    } else if (score === "*") {
        return score;
    }
    return score?.toFixed((score - Math.floor(score)) > 0 ? digits : 0);
};

export const formatPenalty = (contestInfo, penalty) => {
    if (penalty === undefined) {
        return "";
    }
    let mode = contestInfo.penaltyRoundingMode;
    if (mode === "sum_in_seconds" || mode === "last") {
        return Math.floor(penalty / 60) + ":" + (penalty % 60 < 10 ? "0" : "") + (penalty % 60);
    } else {
        return Math.floor(penalty / 60);
    }
};

export const needPenalty = (contestInfo) => contestInfo?.penaltyRoundingMode !== "zero";


export const ProblemCellWrap = styled(Cell)`
  border-bottom: ${props => props.probColor} ${CELL_PROBLEM_LINE_WIDTH} solid;
`;

export const ProblemCell = ({ probData, ...props }) => {
    return <ProblemCellWrap probColor={probData?.color ?? "black"} {...props}>
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


const VerdictCellICPC = ({ result, ...props }) => {
    return <TextShrinkingCell
        background={result.verdict.isAccepted ? VERDICT_OK : VERDICT_NOK}
        align="center"
        text={result.verdict.shortName}
        canGrow={false}
        canShrink={false}
        {...props}
    >
        {result.isFirstToSolveRun && <StarIcon/>}
    </TextShrinkingCell>;
};

const Verdict = PropTypes.shape({
    isAccepted: PropTypes.bool.isRequired,
    isAddingPenalty: PropTypes.bool.isRequired,
    shortName: PropTypes.string.isRequired,
});

const ICPCResult = PropTypes.shape({
    type: PropTypes.string.isRequired,
    verdict: Verdict.isRequired,
    isFirstToSolveRun: PropTypes.bool.isRequired
});

VerdictCellICPC.PropTypes = {
    result: ICPCResult,
};

const VerdictCellIOI = ({ result, ...props }) => {
    if (result.wrongVerdict === undefined) {
        return <TextShrinkingCell
            align="center"
            canGrow={false}
            canShrink={false}
            background={result.difference > 0 ? VERDICT_OK : (result.difference < 0 ? VERDICT_NOK : VERDICT_UNKNOWN)}
            text={result.difference > 0 ? `+${formatScore(result.difference, 1)}` : (result.difference < 0 ? `-${formatScore(-result.difference, 1)}` : "=")}
            {...props}
        >
            {result.isFirstBestRun && <StarIcon/>}
        </TextShrinkingCell>;
    } else {
        return <TextShrinkingCell background={VERDICT_NOK} 
            text={result.wrongVerdict.shortName}
            align="center"
            canGrow={false}
            canShrink={false}
            {...props}
        />;
    }
};

const IOIResult = PropTypes.shape({
    type: PropTypes.string.isRequired,
    score: PropTypes.number.isRequired,
    difference: PropTypes.number.isRequired,
    wrongVerdict: Verdict.isRequired,
    isFirstBestRun: PropTypes.bool.isRequired
});

VerdictCellIOI.PropTypes = {
    result: IOIResult,
};

const VerdictCellInProgressWrap = styled(Cell)`
  position: relative;
`;

const VerdictCellInProgress = ({ percentage, ...props }) => {
    return <VerdictCellInProgressWrap {...props} >
        {percentage !== 0 && <VerdictCellProgressBar width={percentage * 100 + "%"}/>}
    </VerdictCellInProgressWrap>;
};

VerdictCellInProgress.PropTypes = {
    percentage: PropTypes.number.isRequired
};

export const VerdictCell = ({
    runData: data,
    ...props
}) => {
    console.log(data);
    if (data.result === undefined) {
        return <VerdictCellInProgress percentage={data.percentage} {...props}/>;
    }
    if (data.result.type === "ICPC") {
        return <VerdictCellICPC result={data.result} {...props} />;
    } else {
        return <VerdictCellIOI result={data.result} {...props} />;
    }
};

VerdictCell.propTypes = {
    ...Cell.propTypes,
    runData: PropTypes.shape({
        result: PropTypes.oneOf([IOIResult, ICPCResult]),
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

export const TextShrinkingCell = ({ text, font = GLOBAL_DEFAULT_FONT, align = "left", children, ...props }) => {
    const textWidth = getTextWidth(text, font);
    const cellRef = useRef(null);
    const updateScale = useCallback((newCellRef) => {
        if (newCellRef !== null) {
            cellRef.current = newCellRef;
            newCellRef.children[0].style.transform = "";
            const styles = getComputedStyle(newCellRef);
            const haveWidth = (parseFloat(styles.width) - (parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight)));
            const scaleFactor = Math.min(1, haveWidth / textWidth);
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
        {children}
    </TextShrinkingWrap>;
};

TextShrinkingCell.propTypes = {
    ...Cell.propTypes,
    canGrow: PropTypes.bool,
    canShrink: PropTypes.bool,
    text: PropTypes.string.isRequired,
    align: PropTypes.oneOf(["center", "left"])
};

export const formatRank = (rank) => {
    if (rank === undefined || rank == null)
        return "??";
    else if (rank === 0)
        return "*";
    return rank.toString();
};

export const RankCell = ({ rank, medal, ...props }) => {
    return <Cell background={MEDAL_COLORS[medal]} {...props}>
        {formatRank(rank)}
    </Cell>;
};

RankCell.propTypes = {
    ...Cell.propTypes,
    rank: PropTypes.number
};
