import React from "react";
import _ from "lodash";
import { OptimismLevel } from "@shared/api";
import { ContestantInfo } from "./ContestantInfo";
import SubmissionRow from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";
import { useAppSelector } from "@/redux/hooks";
import { isShouldUseDarkColor } from "@/utils/colors";

const ContestantViewCornerWrap = styled.div<{isSmall: boolean}>`
  display: grid;
  grid-template-rows: 1fr ${c.QUEUE_ROW_HEIGHT}px;
  grid-template-columns: auto 150px;
  
  width: auto;
  /*transform: ${props => props.isSmall ? `scale(${c.TEAMVIEW_SMALL_FACTOR})` : ""};*/
  white-space: nowrap;
  /* transform-origin: bottom left; */
  overflow: hidden;
  height: 100%;
`;

export const CornerContestantInfo = styled(ContestantInfo)`
  grid-column-start: 1;
  grid-column-end: 3;
`;

const TasksContainer = styled.div`
    grid-column-start: 1;
    grid-column-end: 3;
    grid-row-start: 1;
    grid-row-end: 2;

    display: flex;
    flex-wrap: wrap;
    flex-direction: column;
    align-content: flex-start;
    /* css trick for perfect TaskRow overflowing: arrange the columns from bottom to top from right to left */
    transform: scale(-1);
    overflow: hidden;
    height: 100%;
`;

const TaskRow = styled.div`
  display: flex;
  flex: 0 0 ${c.QUEUE_ROW_HEIGHT}px; 
  /* css trick for perfect TaskRow overflowing: arrange the columns from bottom to top from right to left */
  transform: scale(-1);
  overflow: hidden;
  &:last-child {
    border-radius: ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS} 0 0;
  }
`;

export const ContestantViewCorner = ({ teamId, isSmall = false, className = null }: {
    teamId: string;
    isSmall: boolean;
    className?: string;
}) => {
    const problemResults = useAppSelector((state) =>
        state.scoreboard[OptimismLevel.normal]?.ids[teamId]?.problemResults.map((r, i) => ({ ...r, index: i })));

    const tasks = useAppSelector(state => state.contestInfo?.info?.problems);
    const contestData = useAppSelector((state) => state.contestInfo.info);

    const results = _.sortBy(problemResults, "lastSubmitTimeMs")
        .filter(result => result.lastSubmitTimeMs !== undefined);
    // const results = [...results2, ...results2];
    const teamData = useAppSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const darkText = isShouldUseDarkColor(teamData?.color ? teamData?.color : c.CONTESTER_BACKGROUND_COLOR);

    return <ContestantViewCornerWrap isSmall={isSmall} className={className}>
        <TasksContainer>
            {results.map((result, i) =>
                <TaskRow key={i}>
                    <SubmissionRow
                        result={result}
                        problemLetter={tasks[result?.index]?.letter}
                        problemColor={tasks[result?.index]?.color}
                        lastSubmitTimeMs={result?.lastSubmitTimeMs}
                        minScore={contestData?.problems[i]?.minScore}
                        maxScore={contestData?.problems[i]?.maxScore}
                        color={darkText ? "#000" : "#FFF"}
                        bg_color={teamData?.color ? teamData?.color : c.CONTESTER_BACKGROUND_COLOR}
                    />
                </TaskRow>
            )}
        </TasksContainer>
        <CornerContestantInfo teamId={teamId} roundBR={results.length === 0} />
    </ContestantViewCornerWrap>;

};
