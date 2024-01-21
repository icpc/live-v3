import React from "react";
import _ from "lodash";
import { SCOREBOARD_TYPES } from "@/consts";
import { ContestantInfo } from "./ContestantInfo";
import SubmissionRow from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";
import { useAppSelector } from "@/redux/hooks";

const ContestantViewCornerWrap = styled.div<{isSmall: boolean}>`
  display: grid;
  grid-auto-rows: ${c.QUEUE_ROW_HEIGHT}px;
  grid-template-columns: auto minmax(100px, 150px);
  
  width: auto;
  /*transform: ${props => props.isSmall ? `scale(${c.TEAMVIEW_SMALL_FACTOR})` : ""};*/
  white-space: nowrap;

  /* transform-origin: bottom left; */
`;
const TaskRow = styled.div`
  display: flex;
  grid-column-end: 3;
  grid-column-start: 2;
  width: 100%;
`;

const CornerContestantInfo = styled(ContestantInfo)`
  grid-column-end: 3;
  grid-column-start: 1;
`;

export const ContestantViewCorner = ({ teamId, isSmall = false, className = null }) => {
    const scoreboardData = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useAppSelector(state => state.contestInfo?.info?.problems);
    const contestData = useAppSelector((state) => state.contestInfo.info);

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
                    // roundT={false}
                    roundB={i === 0}
                />
            </TaskRow>
        )}
        <CornerContestantInfo teamId={teamId} roundBR={results.length === 0} />
    </ContestantViewCornerWrap>;

};
