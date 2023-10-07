import React from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import c from "../../../config";
import { getTeamTaskColor } from "../../../utils/statusInfo";
import { StackedBarsStatistics } from "../../molecules/statistics/StackedBarsStatistics";

const StatisticsWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${c.CONTEST_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  padding: 8px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 7px;
`;

const StatisticsHeader = styled.div`
  font-size: 32px;
  font-weight: 700;
  line-height: 44px;
  color: white;
  width: 100%;
  display: flex;
`;

const StatisticsHeaderTitle = styled.div`
  flex: 1 0 0;
`;

const StatisticsHeaderCaption = styled.div``;


const stackedBarsData = (resultType, tasks, statistics, count) => {
    if (!tasks || !statistics || !count) {
        return {
            legend: [],
            bars: [],
        };
    }

    const bars = [];
    if (resultType === "ICPC") {
        bars.push(...statistics?.map(({result, success, pending, wrong}, index) => ({
            name: tasks[index].letter,
            color: tasks[index].color,
            values: [
                {
                    color: c.VERDICT_OK2,
                    caption: success ? success.toString() : "",
                    value: count ? success / count : 0.0,
                },
                {
                    color: c.VERDICT_UNKNOWN2,
                    caption: pending ? pending.toString() : "",
                    value: count ? pending / count : 0.0,
                },
                {
                    color: c.VERDICT_NOK2,
                    caption: wrong ? wrong.toString() : "",
                    value: count ? wrong / count : 0.0,
                },
            ]
        })));
    } else if (resultType === "IOI") {
        bars.push(...statistics?.map(({ result, success, pending, wrong }, index) => ({
            name: tasks[index].letter,
            color: tasks[index].color,
            values: result.map(({ count: rCount, score }) => ({
                color: getTeamTaskColor(score, tasks[index]?.minScore, tasks[index]?.maxScore),
                value: count ? rCount / count : 0.0,
            })),
        })));
    }
    return {
        legend: [],
        bars: bars,
    };
}

export const Statistics = ({ widgetData: { location } }) => {
    const resultType = useSelector(state => state.contestInfo?.info?.resultType);
    const statistics = useSelector(state => state.statistics.statistics);
    const count = useSelector(state => state.contestInfo?.info?.teams?.length);
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const rowsCount = Math.min(
        tasks?.length ?? 0,
        Math.floor((location.sizeY - 60) / (c.STATISTICS_BAR_HEIGHT_PX + c.STATISTICS_BAR_GAP_PX))
    );

    return (
        <StatisticsWrap>
            <StatisticsHeader>
                <StatisticsHeaderTitle>{c.STATISTICS_TITLE}</StatisticsHeaderTitle>
                <StatisticsHeaderCaption>{c.STATISTICS_CAPTION}</StatisticsHeaderCaption>
            </StatisticsHeader>

            <StackedBarsStatistics data={stackedBarsData(resultType, tasks, statistics, count)} rowsCount={rowsCount}/>
        </StatisticsWrap>
    )
};
export default Statistics;
