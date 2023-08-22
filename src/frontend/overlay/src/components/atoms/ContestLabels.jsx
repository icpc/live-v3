import PropTypes from "prop-types";
import React from "react";
import styled from "styled-components";
import {
    MEDAL_COLORS,
    VERDICT_NOK2,
    VERDICT_OK2,
    VERDICT_UNKNOWN2,
} from "../../config";
import { isShouldUseDarkColor } from "../../utils/colors";
import { ShrinkingBox } from "./ShrinkingBox";
import { formatScore, ICPCResult, IOIResult } from "./ContestCells";
import {
    TeamTaskColor,
    TeamTaskSymbol,
    TeamTaskStatus,
    getStatus,
    getTeamTaskColor,
} from "../../utils/statusInfo";

export const ICPCTaskResult = PropTypes.shape({
    type: PropTypes.string.isRequired,
    pendingAttempts: PropTypes.number.isRequired,
    wrongAttempts: PropTypes.number.isRequired,
    isSolved: PropTypes.bool.isRequired,
    isFirstToSolve: PropTypes.bool.isRequired,
});

export const IOITaskResult = PropTypes.shape({
    type: PropTypes.string.isRequired,
    score: PropTypes.number,
});

const VerdictLabel = styled(ShrinkingBox)`
  background-color: ${({ color }) => color};
  font-size: 14px;
  font-weight: 700;
  display: flex;
  justify-content: center;
  align-items: center;
`

const ICPCVerdictLabel = ({ runResult, className }) => {
    const color = runResult?.verdict.isAccepted ? VERDICT_OK2 : VERDICT_NOK2;
    return <VerdictLabel text={runResult?.verdict.shortName ?? "??"} color={color} align="center" className={className}/>;
};

ICPCVerdictLabel.propTypes = {
    runResult: ICPCResult,
};

const getIOIScoreText = (difference) => {
    if (difference > 0) {
        return [`+${formatScore(difference, 1)}`, VERDICT_OK2];
    }
    if (difference < 0) {
        return [`-${formatScore(-difference, 1)}`, VERDICT_NOK2];
    }
    return ["=", VERDICT_UNKNOWN2];
};

const IOIVerdictLabel = ({ runResult: { wrongVerdict, difference }, ...props }) => {
    const [diffText, diffColor] = getIOIScoreText(difference);
    return <>
        {wrongVerdict !== undefined && <ShrinkingBox text={wrongVerdict ?? "??"} color={VERDICT_NOK2} {...{ Wrapper: FlexedBox2, ...props }}/>}
        {wrongVerdict === undefined && <ShrinkingBox text={diffText ?? "??"} color={diffColor} {...{ Wrapper: FlexedBox2, ...props }}/>}
    </>;
};
IOIVerdictLabel.propTypes = {
    runResult: IOIResult,
};

export const VerdictLabel2 = ({ runResult, ...props }) => {
    return <>
        {runResult.type === "ICPC" && <ICPCVerdictLabel runResult={runResult} {...props}/>}
        {runResult.type === "IOI" && <IOIVerdictLabel runResult={runResult} {...props}/>}
    </>;
};


export const formatRank = (rank) => {
    if (rank === undefined || rank == null)
        return "??";
    else if (rank === 0)
        return "*";
    return rank.toString();
};

const RankLabelWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${({ color }) => color};
  color: ${props => props.dark ? "#000" : "#FFF"};
`;

export const RankLabel = ({ rank, medal, className }) => {
    const color = MEDAL_COLORS[medal];
    const dark = isShouldUseDarkColor(color);
    return <RankLabelWrap color={color} className={className} dark={dark}>
        {formatRank(rank)}
    </RankLabelWrap>;
};

const VerdictCellProgressBar2 = styled.div.attrs(({width}) => ({
    style: {
        width
    }
}))`
  height: 100%;
  transition: width 250ms linear;
  background-color: ${VERDICT_UNKNOWN2};
`;


const VerdictCellInProgressWrap2 = styled.div`
  flex-direction: row;
  justify-content: flex-start;
  height: 100%;
  align-content: center;
  border-radius: 0 16px 16px 0;
  border: 3px solid ${VERDICT_UNKNOWN2};
  box-sizing: border-box;
`;

const VerdictCellInProgress2 = ({ percentage, className }) => {
    return <VerdictCellInProgressWrap2 className={className}>
        {percentage !== 0 && <VerdictCellProgressBar2 width={percentage * 100 + "%"}/>}
    </VerdictCellInProgressWrap2>;
};

VerdictCellInProgress2.propTypes = {
    percentage: PropTypes.number.isRequired
};

export const RunStatusLabel = ({ runInfo, className }) => {
    return <>
        {runInfo.result === undefined && <VerdictCellInProgress2 percentage={runInfo.percentage} className={className}/>}
        {runInfo.result !== undefined && <VerdictLabel2 runResult={runInfo.result} score={runInfo.result.result} className={className}/>}
    </>;
};

RunStatusLabel.propTypes = {
    runInfo: PropTypes.shape({
        result: PropTypes.oneOf([IOIResult, IOIResult]),
        percentage: PropTypes.number.isRequired,
    }),
};

const TaskResultLabelWrapper2 = styled.div`
  font-weight: bold;
  background-color: ${({ color }) => color};
  color: #fff;
`;

// TODO: fts start
const ICPCTaskResultLabel2 = ({ problemResult: r, ...props }) => {
    const status = getStatus(r.isFirstToSolve, r.isSolved, r.pendingAttempts, r.wrongAttempts);
    const attempts = r.wrongAttempts + r.pendingAttempts;
    console.log(r)
    return <>
        {/*{status === TeamTaskStatus.first && <StarIcon/>}*/}
        <TaskResultLabelWrapper2 color={TeamTaskColor[status]} {...props}>
            {TeamTaskSymbol[status]}
            {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
        </TaskResultLabelWrapper2>
    </>;
};

ICPCTaskResultLabel2.propTypes = {
    problemResult: ICPCTaskResult,
};

const IOITaskResultLabel2 = ({ problemResult: r, minScore, maxScore,  ...props }) => {
    return <TaskResultLabelWrapper2 color={getTeamTaskColor(r.score, minScore, maxScore)} { ...props}>
        {formatScore(r?.score)}
    </TaskResultLabelWrapper2>;
};

IOITaskResultLabel2.propTypes = {
    problemResult: IOITaskResult,
    minScore: PropTypes.number,
    maxScore: PropTypes.number,
};

export const TaskResultLabel = ({ problemResult, minScore, maxScore, ...props }) => {
    return <>
        {problemResult.type === "ICPC" && <ICPCTaskResultLabel2 problemResult={problemResult} {...props}/>}
        {problemResult.type === "IOI" && <IOITaskResultLabel2 problemResult={problemResult} minScore={minScore} maxScore={maxScore} {...props}/>}
    </>;
};
