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
    getTeamTaskColor,
} from "../../utils/statusInfo";
import { formatScore } from "../../services/displayUtils";

const VerdictLabel = styled(ShrinkingBox)`
  background-color: ${({ color }) => color};
  font-size: 14px;
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  display: flex;
  justify-content: center;
  align-items: center;
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

const VerdictLabel2 = ({ runResult, ...props }) => {
    return <>
        {runResult.type === "ICPC" && <ICPCVerdictLabel runResult={runResult} {...props}/>}
        {runResult.type === "IOI" && <IOIVerdictLabel runResult={runResult} {...props}/>}
        {/*{runResult.type === "IOI" && <VerdictLabel text={"AA" ?? "??"} color="red" align="center" {...props}/>}*/}
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
  background-color: ${({ color }) => color};
  color: ${props => props.dark ? "#000" : "#FFF"};
`;

export const RankLabel = memo(({ rank, medal, className }) => {
    const color = c.MEDAL_COLORS[medal];
    const dark = isShouldUseDarkColor(color);
    return <RankLabelWrap color={color} className={className} dark={dark} text={formatRank(rank)} />;
});

const VerdictCellProgressBar2 = styled.div.attrs(({ width }) => ({
    style: {
        width
    }
}))`
  height: 100%;
  transition: width 250ms linear;
  background-color: ${c.VERDICT_UNKNOWN};
`;


const VerdictCellInProgressWrap2 = styled.div`
  flex-direction: row;
  justify-content: flex-start;
  height: 100%;
  align-content: center;
  border-radius: 0 16px 16px 0;
  border: 3px solid ${c.VERDICT_UNKNOWN};
  box-sizing: border-box;
`;

const VerdictCellInProgress2 = ({ percentage, className }) => {
    return <VerdictCellInProgressWrap2 className={className}>
        {percentage !== 0 && <VerdictCellProgressBar2 width={percentage * 100 + "%"}/>}
    </VerdictCellInProgressWrap2>;
};

export const RunStatusLabel = ({ runInfo, className }) => {
    return <>
        {runInfo.result === undefined && <VerdictCellInProgress2 percentage={runInfo.percentage} className={className}/>}
        {runInfo.result !== undefined && <VerdictLabel2 runResult={runInfo.result} score={runInfo.result.result} className={className}/>}
    </>;
};

const TaskResultLabelWrapper2 = styled.div`
  font-weight: bold;
  background-color: ${({ color }) => color};
  color: #fff;
`;


const StarIcon = styled.div`
    position: absolute;
    width: ${c.STAR_SIZE}px;
    height: ${c.STAR_SIZE}px;
    mask: url(${star});
    background-position: center;
    background-color: ${({ color }) => color};
`;

const AttemptsOrScoreLabelWrapper = styled.div`
    position: relative;
`;

const defaultColorForStar = "#000";
const greenColor = "#3bba6b";

const ICPCTaskResultLabel2 = ({ problemColor, problemResult: r, ...props }) => {
    const status = getStatus(r.isFirstToSolve, r.isSolved, r.pendingAttempts, r.wrongAttempts);
    const attempts = r.wrongAttempts + r.pendingAttempts;
    return <>
        <TaskResultLabelWrapper2 color={TeamTaskColor[status]} {...props}>
            { status === TeamTaskStatus.first && <StarIcon color={(problemColor === undefined || problemColor === greenColor) ? defaultColorForStar : problemColor}/> }
            <AttemptsOrScoreLabelWrapper>
                {TeamTaskSymbol[status]}
                {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
            </AttemptsOrScoreLabelWrapper>
        </TaskResultLabelWrapper2>
    </>;
};

const IOITaskResultLabel2 = ({ problemColor, problemResult: r, minScore, maxScore,  ...props }) => {
    return <TaskResultLabelWrapper2 color={getTeamTaskColor(r.score, minScore, maxScore)} { ...props}>
        { r.isFirstBest && <StarIcon color={(problemColor === undefined || problemColor === greenColor) ? defaultColorForStar : problemColor}/>}
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
