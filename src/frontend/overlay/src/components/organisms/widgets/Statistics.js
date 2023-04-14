import React, { Fragment } from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import {
    CELL_FONT_FAMILY,
    STATISTICS_BG_COLOR,
    STATISTICS_CELL_MORPH_TIME,
    STATISTICS_OPACITY,
    STATISTICS_STATS_VALUE_COLOR,
    STATISTICS_STATS_VALUE_FONT_FAMILY,
    STATISTICS_STATS_VALUE_FONT_SIZE,
    STATISTICS_TITLE_COLOR,
    STATISTICS_TITLE_FONT_SIZE,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../../config";
import { Cell } from "../../atoms/Cell";
import { ProblemCell } from "../../atoms/ContestCells";
import { getTeamTaskColor } from "./Scoreboard";

const AllDiv = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
`;

const StatisticsWrap = styled.div`
  width: 100%;
  position: absolute;
  bottom: 0px;
  display: flex;
  flex-direction: column;
  opacity: ${STATISTICS_OPACITY};
  background: ${STATISTICS_BG_COLOR};
`;

const Title = styled.div`
  background: ${VERDICT_NOK};
  color: ${STATISTICS_TITLE_COLOR};
  font-size: ${STATISTICS_TITLE_FONT_SIZE};
  text-align: center;
  font-family: ${CELL_FONT_FAMILY}
`;

const Table = styled.div`
  height: 100%;
  display: grid;
  /* stylelint-disable-next-line */
  grid-template-columns: auto 1fr;
`;


const SubmissionStats = styled.div`
  grid-column: 2;
  overflow: hidden;
  text-align: end;
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  height: 100%;
  width: 100%;
  font-size: ${STATISTICS_STATS_VALUE_FONT_SIZE};
  font-family: ${STATISTICS_STATS_VALUE_FONT_FAMILY};
  color: ${STATISTICS_STATS_VALUE_COLOR};
`;

const StatEntry = styled(Cell).attrs(({ targetWidth }) => ({
    style: {
        width: targetWidth,
    }
}))`
  background: ${props => props.color};
  transition: width linear ${STATISTICS_CELL_MORPH_TIME}ms;
  height: 100%;
  overflow: hidden;
  float: left;
  box-sizing: border-box;
  text-align: center;
  font-family: ${CELL_FONT_FAMILY};
  &:before {
    content: '';
    display: inline-block;
  }
`;


const StatisticsProblemCell = styled(ProblemCell)`
  padding: 0 10px;
  box-sizing: border-box;
`;

const getFormattedWidth = (count) => (val, fl) => {
    if (fl) {
        return `calc(max(${val / count * 100}%, ${val === 0 ? 0 : (val + "").length + 1}ch))`;
    } else {
        return `${val / count * 100}%`;
    }
};

const StatEntryContent = styled.div`
   white-space: nowrap;
    display: inline-block;
`;

export const Statistics = () => {
    const statistics = useSelector(state => state.statistics.statistics);
    const resultType = useSelector(state => state.contestInfo?.info?.resultType);
    const count = useSelector(state => state.contestInfo?.info?.teams?.length);
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo?.info);

    const calculator = getFormattedWidth(count);
    return <AllDiv>
        <StatisticsWrap>
            <Title>Statistics</Title>
            <Table>
                {tasks && statistics?.map(({ result }, index) => {
                    const [success, pending, wrong] = result;
                    return <Fragment key={index}>
                        <StatisticsProblemCell probData={tasks[index]}/>
                        {resultType === "ICPC" &&
                        <SubmissionStats>
                            <StatEntry targetWidth={calculator(success, true)} color={VERDICT_OK}>
                                {success}
                            </StatEntry>
                            <StatEntry targetWidth={calculator(pending, true)} color={VERDICT_UNKNOWN}>
                                {pending}
                            </StatEntry>
                            <StatEntry targetWidth={calculator(wrong, true)} color={VERDICT_NOK}>
                                {wrong}
                            </StatEntry>
                        </SubmissionStats>
                        }
                        {resultType !== "ICPC" &&
                            <SubmissionStats>
                                {result.map(({ count, score }, i) => {
                                    return <StatEntry targetWidth={calculator(count, false)} color={getTeamTaskColor(score, contestData?.problems[index]?.minScore, contestData?.problems[index]?.maxScore)} key={i}>
                                    </StatEntry>;
                                })}
                            </SubmissionStats>
                        }

                    </Fragment>;
                })}
            </Table>
        </StatisticsWrap>
    </AllDiv>;
};
export default Statistics;
