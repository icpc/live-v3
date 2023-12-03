import React from "react";
import { useSelector } from "react-redux";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ContestantInfo } from "./ContestantInfo";
import { VerticalSubmissionRow } from "./SubmissionRow";
import styled from "styled-components";
import c from "../../../config";


const ContestantViewVerticalWrap = styled.div`
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

const TaskRow = styled.div`
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

export const ContestantViewLine = ({ teamId, isSmall, className, isTop }) => {

    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);

    const [top, bottom] = isTop ? [null, "0"] : ["0", null];

    return <ContestantViewVerticalWrap isSmall={isSmall} className={className} tasks={scoreboardData?.problemResults.length} top={top} bottom={bottom}>
        <CornerContestantInfo teamId={teamId} />
        {scoreboardData?.problemResults.map((result, i) =>
            <TaskRow key={i} start={i + 2} end={i + 3} index={i} problems={scoreboardData?.problemResults.length} roundTop={isTop} roundBottom={!isTop}>
                <VerticalSubmissionRow
                    isTop={isTop}
                    result={result}
                    problemLetter={tasks[result?.index]?.letter}
                    problemColor={tasks[result?.index]?.color}
                    lastSubmitTimeMs={result?.lastSubmitTimeMs}
                    minScore={contestData?.problems[result.index]?.minScore}
                    maxScore={contestData?.problems[result.index]?.maxScore}

                />
            </TaskRow>
        )}

    </ContestantViewVerticalWrap>;

};
