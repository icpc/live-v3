import React from "react";
import styled from "styled-components";
import { DateTime } from "luxon";
import { TaskResultLabel } from "../../atoms/ContestLabels";
import { CELL_INFO_VERDICT_WIDTH } from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";
import { ContestantRow } from "./ContestantInfo";


const TimeCell = styled.div`
  flex-basis: 70%;
  text-align: center;
`;

export const SubmissionRow = ({ result, lastSubmitTimeMs, minScore, maxScore, problemLetter, problemColor, ...props }) => {
    return <ContestantRow {...props}>
        <TimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</TimeCell>
        <ProblemLabel letter={problemLetter} problemColor={problemColor} />
        <TaskResultLabel problemResult={result} minScore={minScore} maxScore={maxScore} width={CELL_INFO_VERDICT_WIDTH} align={"center"}/>
    </ContestantRow>;
};

export default SubmissionRow;
