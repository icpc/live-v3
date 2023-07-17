import React from "react";
import styled from "styled-components";
import { DateTime } from "luxon";
import { ContestantRow2 } from "../../atoms/ContestantRow2";
import { ProblemCircleLabel, TaskResultLabel2 } from "../../atoms/ContestLabels2";
import { CELL_INFO_VERDICT_WIDTH } from "../../../config";
import { Box2 } from "../../atoms/Box2";


const TimeCell = styled(Box2)`
  flex-basis: 70%;
  text-align: center;
`;

export const SubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, ...props }) => {
    return <ContestantRow2 {...props}>
        <TimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</TimeCell>
        <ProblemCircleLabel letter={problemLetter} problemColor={problemColor} />
        <TaskResultLabel2 problemResult={result} minScore={minScore} maxScore={maxScore} width={CELL_INFO_VERDICT_WIDTH} align={"center"}/>
    </ContestantRow2>;
};

export default SubmissionRow;
