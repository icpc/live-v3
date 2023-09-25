import React from "react";
import styled from "styled-components";
import { DateTime } from "luxon";
import {TaskResultLabel} from "../../atoms/ContestLabels";
import {
    CONTESTER_BACKGROUND_COLOR,
    CONTESTER_ROW_HEIGHT,
    QUEUE_PROBLEM_LABEL_FONT_SIZE
} from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";


const TimeCell = styled.div`
  flex-basis: 70%;
  width: 50px;
  text-align: center;
`;

const QueueProblemLabel = styled(ProblemLabel)`
  width: 28px;
  font-size: ${QUEUE_PROBLEM_LABEL_FONT_SIZE};
  flex-shrink: 0;
`;

const SubmissionRowWrap = styled.div`
  width: 100%;
  height: ${CONTESTER_ROW_HEIGHT};
  background-color: ${CONTESTER_BACKGROUND_COLOR};
  
  display: flex;
  align-items: center;
  border-top-left-radius: ${props => props.roundB ? "16px" : "0px"};
  border-top-right-radius: ${props => props.roundB ? "16px" : "0px"};
  overflow: hidden;
  color: white;
  font-size: 18px;
`;

const SubmissionRowTaskResultLabel = styled(TaskResultLabel)`
  width: 40px;
  height: 100%;
  text-align: center;
  flex-shrink: 0;
`

export const SubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, roundB}) => {

    return <SubmissionRowWrap roundB={roundB}>
            <TimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</TimeCell>
            <QueueProblemLabel letter={problemLetter} problemColor={problemColor} />
            <SubmissionRowTaskResultLabel problemResult={result} minScore={minScore} maxScore={maxScore}/>

    </SubmissionRowWrap>;
};

export default SubmissionRow;
