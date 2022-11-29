import React, { useLayoutEffect } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { PVP_APPEAR_TIME, STATISTICS_BG_COLOR, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { Cell } from "../../atoms/Cell";
import { RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";
import { formatScore } from "../../atoms/ContestCells";
import { getStatus, TeamViewHolder } from "../holder/TeamViewHolder";

// Burn this.
// - Max

const NUMWIDTH = 80;
const NAMEWIDTH = 300;
const STATWIDTH = 80;

const ScoreboardCell = styled(Cell)`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  position: relative;
`;

const ScoreboardStatCell = styled(ScoreboardCell)`
  width: ${STATWIDTH}px;
`;

const TeamInfoWrapper = styled.div`
  display: flex;
  grid-row: 1;
  position: relative;
  height: 100%;
`;

const ScoreboardTaskCellWrap = styled(ScoreboardCell)`
  flex-grow: 1;
  flex-shrink: 1;
  flex-basis: 100%;
  height: 100%;
  padding: 5px;
  min-width: 40px;
`;

const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});

const TeamTaskColor = Object.freeze({
    [TeamTaskStatus.solved]: VERDICT_OK,
    [TeamTaskStatus.failed]: VERDICT_NOK,
    [TeamTaskStatus.untouched]: STATISTICS_BG_COLOR,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const StatisticsProblemCellWithColor = ({ probData, status }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {probData?.letter ?? "??"}
    </ScoreboardTaskCellWrap>;
};


const ScoreboardRowAllWrapper = styled.div`
  display: grid;
  justify-content: start;
  grid-template-rows: 41.5px 41.5px;
  position: relative;
`;

const TaskRowWrapperFirst = styled.div`
  grid-row: 2 / 3;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;

  grid-auto-flow: column;
  position: relative;
`;

const ScoreboardTeamInfoRowFirst = styled.div`
  grid-row: 1 / 2;
`;

const TaskRowWrapperSecond = styled.div`
  grid-row: 1 / 2;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;

  grid-auto-flow: column;
  position: relative;
`;

const ScoreboardTeamInfoRowSecond = styled.div`
  grid-row: 2 / 3;
`;
const TaskRow = styled.div`
  display: flex;
  flex-direction: column;
`;

const ScoreboardRowAllTaskFirst = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <ScoreboardTeamInfoRowFirst>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowFirst>
        <TaskRowWrapperFirst>
            {scoreboardData?.problemResults.flatMap(({
                wrongAttempts,
                pendingAttempts,
                isSolved,
                isFirstToSolve,
                index
            }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]}
                        status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}/>
                </TaskRow>
            )}
        </TaskRowWrapperFirst>
    </ScoreboardRowAllWrapper>;
};

const ScoreboardRowAllTaskSecond = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <TaskRowWrapperSecond>
            {scoreboardData?.problemResults.flatMap(({
                wrongAttempts,
                pendingAttempts,
                isSolved,
                isFirstToSolve,
                index
            }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]}
                        status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                        attempts={wrongAttempts + pendingAttempts}/>
                </TaskRow>
            )}
        </TaskRowWrapperSecond>
        <ScoreboardTeamInfoRowSecond>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowSecond>
    </ScoreboardRowAllWrapper>;
};

const TeamInfo = ({ teamId }) => {
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? "??" : formatScore(scoreboardData.totalScore, 1)}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>

    </TeamInfoWrapper>;
};

const ScoreboardWrapper = styled.div.attrs(({ align }) => ({ style: { justifyContent: align } }))`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: start;
  position: relative;
  flex-direction: column;
  animation: ${props => props.animation} ${PVP_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;

// opacity: ${PVP_OPACITY};

const PVPInfo = styled.div`
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-items: start;
`;

const TeamPVPPInPWrapper = styled.div`
  position: absolute;

  width: ${({ sizeX }) => `${sizeX * 0.361}px`};
  height: ${({ sizeX }) => `${sizeX * 0.52 * 0.4}px`};
  left: 0;
  bottom: ${({ bottom }) => `${bottom}`};
  top: ${({ top }) => `${top}`};

`;

export const PVP = ({ mediaContent, settings, setLoadedComponents, location }) => {

    if (settings.position === "PVP_TOP") {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamPVPPInPWrapper bottom={"80px"} top={"auto"}
                        sizeX={location.sizeX}>{component}</TeamPVPPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo>
                    <ScoreboardWrapper align={"end"}>
                        <ScoreboardRowAllTaskFirst teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    } else {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamPVPPInPWrapper top={"80px"} bottom={"auto"}
                        sizeX={location.sizeX}>{component}</TeamPVPPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo>
                    <ScoreboardWrapper align={"start"}>
                        <ScoreboardRowAllTaskSecond teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    }
};
PVP.ignoreAnimation = true;
PVP.overrideTimeout = PVP_APPEAR_TIME;
export default PVP;
