import React from "react";
import { OptimismLevel } from "@shared/api";
import { ContestantInfo } from "./ContestantInfo";
import { VerticalSubmissionRow } from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";
import { useAppSelector } from "@/redux/hooks";

type ContestantViewVerticalWrapProps = {
    top: string;
    bottom: string;
    tasks: number;
    start?: string;
    end?: string;
    taskWidth: number;
};
const ContestantViewVerticalWrap = styled.div<ContestantViewVerticalWrapProps>`
    position: absolute;
    top: ${(props) => props.top};
    bottom: ${(props) => props.bottom};

    display: grid;
    grid-auto-rows: ${c.PVP_TABLE_ROW_HEIGHT}px;
    grid-template-columns: auto repeat(
            ${(props) => props.tasks},
            ${(props) => props.taskWidth}px
        );

    width: auto;
    white-space: nowrap;

    /* transform-origin: bottom left; */
`;

type TaskRowProps = {
    end?: number;
    start?: number;
    $isTop?: boolean;
};

const TaskRow = styled.div<TaskRowProps>`
    overflow: hidden;
    display: flex;
    grid-column-end: ${(props) => props.end};
    grid-column-start: ${(props) => props.start};
    grid-row: 1 / 4;

    width: 100%;
    &:nth-child(2) {
        border-radius: ${(props) => (props.$isTop ? c.GLOBAL_BORDER_RADIUS : 0)}
            0 0 ${(props) => (props.$isTop ? 0 : c.GLOBAL_BORDER_RADIUS)};
    }
`;

const CornerContestantInfo = styled(ContestantInfo)`
    grid-row: 2 / 3;
    border-radius: ${c.GLOBAL_BORDER_RADIUS} 0 0 ${c.GLOBAL_BORDER_RADIUS};
`;

interface ContestantViewLineProps {
    teamId: string;
    tasksContainerY?: number;
    className?: string;
    isTop?: boolean;
}

export const ContestantViewLine = ({
    teamId,
    tasksContainerY,
    className,
    isTop,
}: ContestantViewLineProps) => {
    const scoreboardData = useAppSelector(
        (state) =>
            state.scoreboard[OptimismLevel.normal]?.ids &&
            state.scoreboard[OptimismLevel.normal].ids[teamId],
    );
    const tasks = useAppSelector((state) => state.contestInfo?.info?.problems);
    const contestData = useAppSelector((state) => state.contestInfo?.info);
    const teamData = useAppSelector(
        (state) => state.contestInfo.info?.teamsId[teamId],
    );

    const [top, bottom] = isTop ? [null, "0"] : ["0", null];

    const taskWidth = tasksContainerY
        ? Math.max(
              c.PVP_TEAM_STATUS_TASK_WIDTH,
              tasksContainerY / tasks?.length,
          )
        : c.PVP_TEAM_STATUS_TASK_WIDTH;

    return (
        <ContestantViewVerticalWrap
            className={className}
            tasks={scoreboardData?.problemResults?.length}
            taskWidth={taskWidth}
            top={top}
            bottom={bottom}
        >
            <CornerContestantInfo teamId={teamId} />
            {scoreboardData?.problemResults?.map((result, i) => (
                <TaskRow key={i} start={i + 2} end={i + 3} $isTop={isTop}>
                    <VerticalSubmissionRow
                        teamData={teamData}
                        isTop={isTop}
                        result={result}
                        problemLetter={tasks && tasks[i]?.letter}
                        problemColor={tasks && tasks[i]?.color}
                        lastSubmitTimeMs={result?.lastSubmitTimeMs}
                        minScore={
                            contestData && contestData.problems[i]?.minScore
                        }
                        maxScore={
                            contestData && contestData.problems[i]?.maxScore
                        }
                    />
                </TaskRow>
            ))}
        </ContestantViewVerticalWrap>
    );
};
