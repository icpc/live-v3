import React from "react";
import _ from "lodash";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ContestantInfo } from "./ContestantInfo";
import SubmissionRow from "./SubmissionRow";
import styled from "styled-components";
import {QUEUE_ROW_HEIGHT2, TEAMVIEW_SMALL_FACTOR} from "../../../config";

const ScoreboardColumnWrapper = styled.div`
  display: grid;
  z-index: 1;
  grid-template-columns: auto minmax(100px, 150px);
  grid-auto-rows: ${QUEUE_ROW_HEIGHT2}px;
  grid-column-end: 3;
  grid-row-start: 1;
  grid-row-end: ${props => props.hasPInP ? 2 : 3};
  justify-self: end;
  align-self: end;
  width: auto;
  transform-origin: bottom left;
  /*transform: ${props => props.isSmall ? `scale(${TEAMVIEW_SMALL_FACTOR})` : ""};*/
  white-space: nowrap;
`;
const TeamInfoRow = styled.div`
  grid-column-start: 1;
  grid-column-end: 3;
`;
const TaskRow = styled.div`
  display: flex;
  width: 100%;
  grid-column-start: 2;
  grid-column-end: 3;
`;

export const ContestantViewCorner = ({ teamId, isSmall, hasPInP }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);

    const results = _.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs")
        .filter(result => result.lastSubmitTimeMs !== undefined);
    return <ScoreboardColumnWrapper isSmall={isSmall} hasPInP={hasPInP}>
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
        <TeamInfoRow >
            <ContestantInfo teamId={teamId} roundBR={results.length === 0} />
        </TeamInfoRow>

    </ScoreboardColumnWrapper>;

};
