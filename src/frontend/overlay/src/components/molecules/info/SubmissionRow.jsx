import React from "react";
import styled from "styled-components";
import { DateTime } from "luxon";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import {ScoreboardTaskResultLabel} from "../../organisms/widgets/Scoreboard";


const TimeCell = styled.div`
  flex-basis: 70%;
  width: 50px;
  text-align: center;
`;

const QueueProblemLabel = styled(ProblemLabel)`
  width: 28px;
  font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
  flex-shrink: 0;
`;

const SubmissionRowWrap = styled.div`
  width: 100%;
  height: ${c.CONTESTER_ROW_HEIGHT};
  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
  
  display: flex;
  align-items: center;
  border-top-left-radius: ${props => props.roundB ? "16px" : "0px"};
  border-top-right-radius: ${props => props.roundB ? "16px" : "0px"};
  overflow: hidden;
  color: white;
  font-size: ${c.CONTESTER_FONT_SIZE};
`;

const SubmissionColumnWrap = styled.div`
  width: 100%;
  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
  
  display: grid;
  align-items: center;
  border-top-left-radius: ${props => props.roundB ? "16px" : "0px"};
  border-top-right-radius: ${props => props.roundB ? "16px" : "0px"};
  overflow: hidden;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr 1fr 1fr;

  grid-auto-flow: row;
  color: white;
  font-size: ${c.CONTESTER_FONT_SIZE};
`;

const SubmissionRowTaskResultLabel = styled(ScoreboardTaskResultLabel)`
  width: 40px;
  height: 100%;
  text-align: center;
  flex-shrink: 0;
`;

export const SubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, roundB }) => {
    return <SubmissionRowWrap roundB={roundB}>
        <TimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</TimeCell>
        <QueueProblemLabel letter={problemLetter} problemColor={problemColor} />
        <SubmissionRowTaskResultLabel problemResult={result} minScore={minScore} maxScore={maxScore}/>

    </SubmissionRowWrap>;
};

const PVPProblemLabel = styled(QueueProblemLabel)`
  width: 100%;
  order: ${props => props.isTop ? 3 : 1};
`

const PVPResultLabel = styled(ScoreboardTaskResultLabel)`
  order: ${props => props.isTop ? 1 : 3};
  
`

const PVPTimeCell = styled(TimeCell)`
  order: 2;
`

export const VerticalSubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, isTop }) => {
    if (result.lastSubmitTimeMs === undefined) {
        return <SubmissionColumnWrap>
            <div style={{order:2}}/>
            <div style={{order:2}}/>

            <PVPProblemLabel letter={problemLetter} problemColor={problemColor} isTop={isTop}/>
        </SubmissionColumnWrap>;
    }
    return <SubmissionColumnWrap>
        <PVPResultLabel problemResult={result} minScore={minScore} maxScore={maxScore} isTop={isTop}/>
        <PVPTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</PVPTimeCell>
        <PVPProblemLabel letter={problemLetter} problemColor={problemColor} isTop={isTop}/>
    </SubmissionColumnWrap>;
};

export default SubmissionRow;
