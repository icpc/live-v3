import React from "react";
import { SCOREBOARD_TYPES } from "@/consts";
import { ContestantInfo } from "./ContestantInfo";
import { VerticalSubmissionRow } from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";
import { useAppSelector } from "@/redux/hooks";

type ContestantViewVerticalWrapProps = {
    top: string,
    bottom: string,
    tasks: number,
    isSmall: boolean,
    end?: string,
    start?: string
};
const ContestantViewVerticalWrap = styled.div<ContestantViewVerticalWrapProps>`
  position: absolute;
  top: ${props => props.top};
  bottom: ${props => props.bottom};

  display: grid;
  grid-auto-rows: ${c.PVP_TABLE_ROW_HEIGHT}px;
  grid-template-columns: auto repeat(${props => props.tasks}, 50px);

  
  width: auto;
  /*transform: ${props => props.isSmall ? `scale(${c.TEAMVIEW_SMALL_FACTOR})` : ""};*/
  white-space: nowrap;

  /* transform-origin: bottom left; */
`;

type TaskRowProps = {
    end?: number,
    start?: number,
    index: number,
    roundTop: boolean,
    problems: number,
    roundBottom: boolean
}

const TaskRow = styled.div<TaskRowProps>`
  overflow: hidden;
  display: flex;
  grid-column-end: ${props => props.end};
  grid-column-start: ${props => props.start};
  grid-row: 1 / 4;

  width: 100%;

  border-radius: ${props => props.index === 0 && props.roundTop ? "16px" : 0} ${props => props.index === props.problems - 1 && props.roundTop ? "16px" : 0}  ${props => props.index === props.problems - 1 && props.roundBottom ? "16px" : 0} ${props => props.index === 0 && props.roundBottom ? "16px" : 0};
`;

const CornerContestantInfo = styled(ContestantInfo)`
  grid-row: 2 / 3;
  border-radius: ${c.GLOBAL_BORDER_RADIUS} 0 0  ${c.GLOBAL_BORDER_RADIUS};
`;

interface ContestantViewLineProps {
    teamId: number,
    isSmall?: boolean,
    className?: string,
    isTop?: boolean
}

export const ContestantViewLine = ({ teamId, isSmall, className, isTop }: ContestantViewLineProps) => {
    const scoreboardData = useAppSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    // for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
    //     scoreboardData.problemResults[i]["index"] = i;
    // }
    const tasks = useAppSelector(state => state.contestInfo?.info?.problems);
    const contestData = useAppSelector((state) => state.contestInfo.info);

    const [top, bottom] = isTop ? [null, "0"] : ["0", null];

    return <ContestantViewVerticalWrap isSmall={isSmall} className={className} tasks={scoreboardData?.problemResults.length} top={top} bottom={bottom}>
        <CornerContestantInfo teamId={teamId} />
        {scoreboardData?.problemResults.map((result, i) =>
            <TaskRow key={i} start={i + 2} end={i + 3} index={i} problems={scoreboardData?.problemResults.length} roundTop={isTop} roundBottom={!isTop}>
                <VerticalSubmissionRow
                    isTop={isTop}
                    result={result}
                    problemLetter={tasks[i]?.letter}
                    problemColor={tasks[i]?.color}
                    lastSubmitTimeMs={result?.lastSubmitTimeMs}
                    minScore={contestData?.problems[i]?.minScore}
                    maxScore={contestData?.problems[i]?.maxScore}

                />
            </TaskRow>
        )}

    </ContestantViewVerticalWrap>;

};
