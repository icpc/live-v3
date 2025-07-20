import React, { memo } from "react";
import styled from "styled-components";
import c from "../../config";
import { isShouldUseDarkColor } from "../../utils/colors";
import { ShrinkingBox } from "./ShrinkingBox";
import star from "../../assets/icons/scoreboard_star.svg";

import {
    TeamTaskColor,
    TeamTaskSymbol,
    TeamTaskStatus,
    getStatus,
    getIOIColor,
} from "../../utils/statusInfo";
import { formatScore } from "../../services/displayUtils";

const VerdictLabel = styled(ShrinkingBox)`
  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 14px;
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};

  background-color: ${({ color }) => color};
`;

const ICPCVerdictLabel = ({ runResult, className }) => {
    const color = runResult?.verdict.isAccepted ? c.VERDICT_OK : c.VERDICT_NOK;
    return <VerdictLabel text={runResult?.verdict.shortName ?? "??"} color={color} align="center" className={className}/>;
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
        {wrongVerdict !== undefined && <VerdictLabel text={wrongVerdict.shortName ?? "??"} color={c.VERDICT_NOK} align="center" {...props}/>}
        {wrongVerdict === undefined && <VerdictLabel text={diffText ?? "??"} color={diffColor} align="center" {...props}/>}
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

export const RankLabel = memo(({ rank, medal, className, bg_color }) => {
    const color = c.MEDAL_COLORS[medal?.toLowerCase()] ? c.MEDAL_COLORS[medal?.toLowerCase()] : bg_color;
    const dark = isShouldUseDarkColor(color);
    return <RankLabelWrap color={color} className={className} dark={dark} text={formatRank(rank)} />;
});

const VerdictCellProgressBar2 = styled.div.attrs(({ width }) => ({
    style: {
        width
    }
}))`
  height: 100%;
  background-color: ${c.VERDICT_UNKNOWN};
  transition: width 250ms linear;
`;


const VerdictCellInProgressWrap2 = styled.div`
  flex-direction: row;
  align-content: center;
  justify-content: flex-start;

  box-sizing: border-box;
  height: 100%;

  border: 3px solid ${c.VERDICT_UNKNOWN};
  border-radius: 0 16px 16px 0;
`;

const VerdictCellInProgress2 = ({ percentage, className }) => {
    return <VerdictCellInProgressWrap2 className={className}>
        {percentage !== 0 && <VerdictCellProgressBar2 width={percentage * 100 + "%"}/>}
    </VerdictCellInProgressWrap2>;
};

export const RunStatusLabel = ({ runInfo, className }) => {
    return <>
        {runInfo.result.type === "ICPC" && <ICPCVerdictLabel runResult={runInfo.result} className={className}/>}
        {runInfo.result.type === "IOI" && <IOIVerdictLabel runResult={runInfo.result} className={className}/>}
        {runInfo.result.type === "IN_PROGRESS" && <VerdictCellInProgress2 percentage={runInfo.result.testedPart} className={className}/>}
    </>;
};

const TaskResultLabelWrapper2 = styled.div`
  font-weight: bold;
  color: #fff;
  background-color: ${({ color }) => color};
`;

const StarIcon = styled.div`
    width: ${c.STAR_SIZE}px;
    height: ${c.STAR_SIZE}px;
    /* stylelint-disable-next-line plugin/no-unsupported-browser-features */
    mask: url("${star}");
    background-position: center;
    background-color: ${({ color }) => color};
`;

const AttemptsOrScoreLabelWrapper = styled.div`
    position: absolute;
`;

const defaultColorForStar = "#F9A80D";

const ICPCTaskResultLabel2 = ({ problemColor, problemResult: r, ...props }) => {
    const status = getStatus(r.isFirstToSolve, r.isSolved, r.pendingAttempts, r.wrongAttempts);
    const attempts = r.wrongAttempts + r.pendingAttempts;
    return <>
        <TaskResultLabelWrapper2 color={TeamTaskColor[status]} {...props}>
            { status === TeamTaskStatus.first &&
                <StarIcon color={problemColor === undefined ? defaultColorForStar : problemColor}/> }
            <AttemptsOrScoreLabelWrapper>
                {TeamTaskSymbol[status]}
                {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
            </AttemptsOrScoreLabelWrapper>
        </TaskResultLabelWrapper2>
    </>;
};

const IOITaskResultLabel2 = ({ problemColor, problemResult: r, minScore, maxScore,  ...props }) => {
    return <TaskResultLabelWrapper2 color={getIOIColor(r.score, minScore, maxScore)} { ...props}>
        { r.isFirstBest && <StarIcon color={problemColor === undefined ? defaultColorForStar : problemColor}/>}
        <AttemptsOrScoreLabelWrapper>
            {formatScore(r?.score)}
        </AttemptsOrScoreLabelWrapper>
    </TaskResultLabelWrapper2>;
};

export const TaskResultLabel = memo(({ problemResult, ...props }) => {
    return <>
        {problemResult.type === "ICPC" && <ICPCTaskResultLabel2 problemResult={problemResult} {...props}/>}
        {problemResult.type === "IOI" && <IOITaskResultLabel2 problemResult={problemResult}  {...props}/>}
    </>;
});
