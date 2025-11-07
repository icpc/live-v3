import React from "react";
import styled, { css, keyframes } from "styled-components";
import c from "../../config";
import { isShouldUseDarkColor } from "../../utils/colors";
import { ShrinkingBox } from "./ShrinkingBox";

import {
    TeamTaskColor,
    TeamTaskSymbol,
    TeamTaskStatus,
    getStatus,
    getIOIColor,
} from "../../utils/statusInfo";
import { formatScore } from "../../services/displayUtils";

const shimmerAnimation = keyframes`
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 100% 0;
  }
`;

const VerdictLabel = styled(ShrinkingBox)`
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: ${c.VERDICT_LABEL_FONT_SIZE};
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  background-color: ${({ color }) => color};
`;

const ICPCVerdictLabel = ({ runResult, className }) => {
    const color = runResult?.verdict.isAccepted ? c.VERDICT_OK : c.VERDICT_NOK;
    return <VerdictLabel
        text={runResult?.verdict.shortName ?? "??"}
        color={color}
        align="center"
        className={className}
    />;
};

const getIOIScoreText = (difference) => {
    if (difference > 0) {
        return [`+${formatScore(difference, 1)}`, c.VERDICT_OK];
    }
    if (difference < 0) {
        return [`-${formatScore(-difference, 1)}`, c.VERDICT_NOK];
    }
    return ["=", c.VERDICT_UNKNOWN];
};

const IOIVerdictLabel = ({ runResult: { wrongVerdict, difference }, ...props }) => {
    const [diffText, diffColor] = getIOIScoreText(difference);
    return <>
        {wrongVerdict !== undefined &&
            <VerdictLabel
                text={wrongVerdict.shortName ?? "??"}
                color={c.VERDICT_NOK}
                align="center"
                {...props}
            />
        }
        {wrongVerdict === undefined &&
            <VerdictLabel
                text={diffText ?? "??"}
                color={diffColor}
                align="center"
                {...props}
            />
        }
    </>;
};

const formatRank = (rank) => {
    if (rank === undefined || rank == null)
        return "??";
    else if (rank === 0)
        return "*";
    return rank.toString();
};

const RankLabelWrap = styled(ShrinkingBox)`
  display: flex;
  align-items: center;
  justify-content: center;

  color: ${props => props.dark ? "#000" : "#FFF"};

  background-color: ${({ color }) => color};
`;

export const RankLabel = ({ rank, medal, className, bg_color }) => {
    const color = c.MEDAL_COLORS[medal?.toLowerCase()] ? c.MEDAL_COLORS[medal?.toLowerCase()] : bg_color;
    const dark = isShouldUseDarkColor(color);
    return <RankLabelWrap color={color} className={className} dark={dark} text={formatRank(rank)} />;
};

const VerdictCellProgressBar2 = styled.div.attrs(({ width }) => ({
    style: {
        width
    }
}))`
  height: 100%;
  background-color: ${c.VERDICT_UNKNOWN};
  transition: width ${c.VERDICT_CELL_TRANSITION_TIME} linear;
`;

const VerdictCellInProgressWrap2 = styled.div`
  flex-direction: row;
  align-content: center;
  justify-content: flex-start;

  box-sizing: border-box;
  height: 100%;

  border: 3px solid ${c.VERDICT_UNKNOWN};
  border-radius: 0 ${c.VERDICT_CELL_BRODER_RADIUS} ${c.VERDICT_CELL_BRODER_RADIUS} 0;
`;

const VerdictCellInProgress2 = ({ percentage, className }) => {
    return <VerdictCellInProgressWrap2 className={className}>
        {percentage !== 0 && <VerdictCellProgressBar2 width={percentage * 100 + "%"}/>}
    </VerdictCellInProgressWrap2>;
};

export const RunStatusLabel = ({ runInfo, className }) => {
    return <>
        {runInfo.result.type === "ICPC" &&
            <ICPCVerdictLabel
                runResult={runInfo.result}
                className={className}
            />
        }
        {runInfo.result.type === "IOI" &&
            <IOIVerdictLabel
                runResult={runInfo.result}
                className={className}
            />
        }
        {runInfo.result.type === "IN_PROGRESS" &&
            <VerdictCellInProgress2
                percentage={runInfo.result.testedPart}
                className={className}
            />
        }
    </>;
};

const TaskResultLabelWrapper2 = styled.div`
  font-weight: bold;
  color: #fff;

  ${({ color, isShimmering, problemColor, isFake }) => isShimmering && !isFake ? css`
background: linear-gradient(
  90deg,
  ${problemColor || '#4a90e2'} 0%,
  ${problemColor || '#4a90e2'} 10%,
  #fff 50%,
  ${problemColor || '#4a90e2'} 90%,
  ${problemColor || '#4a90e2'} 100%
);
        background-size: 200% 100%;
    animation: ${shimmerAnimation} 4s linear infinite;
    color: #fff;
    text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  ` : css`
    background-color: ${color};
  `}
`;
const AttemptsOrScoreLabelWrapper = styled.div`
    position: absolute;
`;

const ICPCTaskResultLabel2 = ({ problemColor, problemResult: r, problemLetter, ...props }) => {
    const status = getStatus(r.isFirstToSolve, r.isSolved, r.pendingAttempts, r.wrongAttempts);
    const attempts = r.wrongAttempts + r.pendingAttempts;
    const isShimmering = status === TeamTaskStatus.first;

    return <>
        <TaskResultLabelWrapper2
            color={TeamTaskColor[status]}
            isShimmering={isShimmering}
            problemColor={problemColor}
            {...props}
        >
            <AttemptsOrScoreLabelWrapper>
                {TeamTaskSymbol[status]}
                {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
            </AttemptsOrScoreLabelWrapper>
        </TaskResultLabelWrapper2>
    </>;
};

const IOITaskResultLabel2 = ({ problemColor, problemResult: r, problemLetter, minScore, maxScore, ...props }) => {
    const isShimmering = r.isFirstBest;

    return <TaskResultLabelWrapper2
        color={getIOIColor(r.score, minScore, maxScore)}
        isShimmering={isShimmering}
        problemColor={problemColor}
        {...props}
    >
        <AttemptsOrScoreLabelWrapper>
            {formatScore(r?.score)}
        </AttemptsOrScoreLabelWrapper>
    </TaskResultLabelWrapper2>;
};

export const TaskResultLabel = ({ problemResult, problemLetter, ...props }) => {
    return <>
        {problemResult.type === "ICPC" && <ICPCTaskResultLabel2 problemResult={problemResult} problemLetter={problemLetter} {...props}/>}
        {problemResult.type === "IOI" && <IOITaskResultLabel2 problemResult={problemResult} problemLetter={problemLetter} {...props}/>}
    </>;
};
