import React from "react";
import _ from "lodash";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ContestantInfo } from "./ContestantInfo";
import SubmissionRow from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";

const ContestantViewCornerWrap = styled.div`
  display: grid;
  
  grid-template-columns: auto minmax(100px, 150px);
  grid-auto-rows: ${c.QUEUE_ROW_HEIGHT}px;
  
  width: auto;
  //transform-origin: bottom left;
  /*transform: ${props => props.isSmall ? `scale(${c.TEAMVIEW_SMALL_FACTOR})` : ""};*/
  white-space: nowrap;
`;
const TaskRow = styled.div`
  display: flex;
  width: 100%;
  grid-column-start: 2;
  grid-column-end: 3;
`;

const CornerContestantInfo = styled(ContestantInfo)`
  grid-column-start: 1;
  grid-column-end: 3;
`;

export const ContestantViewCorner = ({ teamId, isSmall, className }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);

    const results = _.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs")
        .filter(result => result.lastSubmitTimeMs !== undefined);
    return <ContestantViewCornerWrap isSmall={isSmall} className={className}>
        {results.map((result, i) =>
            <TaskRow key={i}>
                <SubmissionRow
                    result={result}
                    problemLetter={tasks[result?.index]?.letter}
                    problemColor={tasks[result?.index]?.color}
                    lastSubmitTimeMs={result?.lastSubmitTimeMs}
                    minScore={contestData?.problems[result.index]?.minScore}
                    maxScore={contestData?.problems[result.index]?.maxScore}
                    roundT={false}
                    roundB={i === 0}
                />
            </TaskRow>
        )}
        <CornerContestantInfo teamId={teamId} roundBR={results.length === 0} />
    </ContestantViewCornerWrap>;

};
