import React, { Fragment } from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import {
    CELL_FONT_FAMILY,
    STATISTICS_BG_COLOR,
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

const StatisticsWrap = styled.div`
  width: 100%;
  height: 100%;
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
  grid-template-columns: auto 1fr;
`;


const SubmissionStats = styled.div`
  grid-column: 2;
  overflow: hidden;
  text-align: end;
  font-size: ${STATISTICS_STATS_VALUE_FONT_SIZE};
  font-family: ${STATISTICS_STATS_VALUE_FONT_FAMILY};
  color: ${STATISTICS_STATS_VALUE_COLOR};
`;

const StatEntry = styled(Cell).attrs(({ width }) => ({ style: { width: width } }))`
  background: ${props => props.color};
  transition: width linear 200ms;

  height: 100%;
  overflow: hidden;
  float: left;
  text-align: center;

`;

const StatisticsText = styled.div`
  margin: 0 0.1rem 0 0.1rem;
  text-align: center;
  font-family: ${CELL_FONT_FAMILY}
`;

const StatisticsProblemCell = styled(ProblemCell)`
  padding: 0 10px;
  box-sizing: border-box;
`;

export const Statistics = () => {
    const statistics = useSelector(state => state.statistics.statistics);
    const count = useSelector(state => state.contestInfo?.info?.teams?.length);
    const tasks = useSelector(state => state.contestInfo?.info?.problems);

    return <StatisticsWrap>
        <Title>Statistics</Title>
        <Table>
            {statistics?.map(({ success, wrong, pending }, index) =>
                <Fragment key={index}>
                    <StatisticsProblemCell probData={tasks[index]}/>
                    <SubmissionStats>
                        <StatEntry width={success / count * 100 + "%"} color={VERDICT_OK}>
                            <StatisticsText>{success}</StatisticsText>
                        </StatEntry>
                        <StatEntry width={pending / count * 100 + "%"} color={VERDICT_UNKNOWN}>
                            <StatisticsText>
                                {pending}
                            </StatisticsText>
                        </StatEntry>
                        <StatEntry width={wrong / count * 100 + "%"} color={VERDICT_NOK}>
                            <StatisticsText>
                                {wrong}
                            </StatisticsText>
                        </StatEntry>
                    </SubmissionStats>
                </Fragment>
            )}
        </Table>
    </StatisticsWrap>;
};
export default Statistics;
