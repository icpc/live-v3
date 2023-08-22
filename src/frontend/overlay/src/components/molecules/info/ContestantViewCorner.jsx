import React from "react";
import _ from "lodash";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ContestantInfo } from "./ContestantInfo";
import SubmissionRow from "./SubmissionRow";
import styled from "styled-components";
import { TEAMVIEW_SMALL_FACTOR } from "../../../config";

const ScoreboardColumnWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(2, auto);
  grid-auto-rows: 1fr;
  position: absolute;
  transform-origin: top right;
  transform: ${props => props.isSmall ? `scale(${TEAMVIEW_SMALL_FACTOR})` : ""};
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

export const ContestantViewCorner = ({ teamId, isSmall }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);

    const results = _.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs")
        .filter(result => result.lastSubmitTimeMs !== undefined);
    return <ScoreboardColumnWrapper isSmall={isSmall}>
        <TeamInfoRow >
            <ContestantInfo teamId={teamId} roundBR={results.length === 0} />
        </TeamInfoRow>
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
                    roundB={i + 1 === results.length}
                />
            </TaskRow>
        )}
    </ScoreboardColumnWrapper>;

};
