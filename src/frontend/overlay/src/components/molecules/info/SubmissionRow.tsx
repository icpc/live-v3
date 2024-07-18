import React from "react";
import styled from "styled-components";
import { DateTime } from "luxon";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { ScoreboardTaskResultLabel } from "../../organisms/widgets/Scoreboard";


const TimeCell = styled.div`
  flex-basis: 70%;
  width: 50px;
  text-align: center;
`;

const QueueProblemLabel = styled(ProblemLabel)`
  flex-shrink: 0;
  width: 28px;
  font-size: ${c.QUEUE_PROBLEM_LABEL_FONT_SIZE};
`;

const SubmissionRowWrap = styled.div<{roundB: boolean}>`
  overflow: hidden;
  display: flex;
  align-items: center;

  width: 100%;
  height: ${c.CONTESTER_ROW_HEIGHT};

  font-size: ${c.CONTESTER_FONT_SIZE};
  color: white;

  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
  border-top-left-radius: ${props => props.roundB ? "16px" : "0px"};
  border-top-right-radius: ${props => props.roundB ? "16px" : "0px"};
`;

const SubmissionColumnWrap = styled.div`
  overflow: hidden;
  display: grid;
  grid-auto-flow: row;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr 1fr 1fr;
  align-items: center;

  width: 100%;

  font-size: ${c.CONTESTER_FONT_SIZE};
  color: white;

  background-color: ${c.CONTESTER_BACKGROUND_COLOR};
`;
// border-top-left-radius: ${props => props.roundB ? "16px" : "0px"};
//   border-top-right-radius: ${props => props.roundB ? "16px" : "0px"};

const SubmissionRowTaskResultLabel = styled(ScoreboardTaskResultLabel)`
  flex-shrink: 0;
  width: 40px;
  height: 100%;
  text-align: center;
`;

export const SubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, roundB }) => {
    return <SubmissionRowWrap roundB={roundB}>
        <TimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</TimeCell>
        <QueueProblemLabel letter={problemLetter} problemColor={problemColor} />
        <SubmissionRowTaskResultLabel problemResult={result} minScore={minScore} maxScore={maxScore}/>
    </SubmissionRowWrap>;
};

const PVPProblemLabel = styled(QueueProblemLabel)`
  order: ${props => props.isTop ? 3 : 1};
  width: 100%;
`;

const PVPResultLabel = styled(ScoreboardTaskResultLabel)`
  order: ${props => props.isTop ? 1 : 3};
  
`;

const PVPTimeCell = styled(TimeCell)`
  order: 2;
`;

export const VerticalSubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, isTop }) => {
    if (!result || !problemColor || !problemLetter || !lastSubmitTimeMs) {
        return <SubmissionColumnWrap>
            <div style={{ order: 2 }}/>
            <div style={{ order: 2 }}/>
            <PVPProblemLabel letter={problemLetter} problemColor={problemColor} isTop={isTop}/>
        </SubmissionColumnWrap>;
    }
    return <SubmissionColumnWrap>
        <PVPResultLabel problemResult={result} minScore={minScore} maxScore={maxScore} isTop={isTop}/>
        <PVPTimeCell>{DateTime.fromMillis(-1).toFormat("H:mm")}</PVPTimeCell>
        <PVPProblemLabel letter={problemLetter} problemColor={problemColor} isTop={isTop}/>
    </SubmissionColumnWrap>;
};

export default SubmissionRow;
