import React from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import c from "../../../config";
import { getTeamTaskColor } from "../../../utils/statusInfo";
import { StackedBarsStatistics } from "../../molecules/statistics/StackedBarsStatistics";
import { StatisticsLegends } from "../../molecules/statistics/StatisticsLegends";

const StatisticsWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${c.CONTEST_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  padding: 8px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const StatisticsHeader = styled.div`
  font-size: 32px;
  line-height: 44px;
  color: white;
  width: 100%;
  display: flex;
`;

const StatisticsHeaderWrapper = styled.div`
  flex: 1 0 0;
  display: flex;
  gap: 16px;
`

const StatisticsHeaderTitle = styled.div`
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
`;

const StatisticsHeaderCaption = styled.div``;


const stackedBarsData = (resultType, tasks, statistics, count) => {
    if (!tasks || !statistics || !count) {
        return {
            legends: [],
            bars: [],
        };
    }

    const legends = [];
    const bars = [];
    if (resultType === "ICPC") {
        legends.push({
            caption: "solved",
            color: c.VERDICT_OK2,
        });
        legends.push({
            caption: "pending",
            color: c.VERDICT_UNKNOWN2,
        });
        legends.push({
            caption: "incorrect",
            color: c.VERDICT_NOK2,
        });
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
        legends.push({
            caption: "max score",
            color: c.VERDICT_OK2,
        });
        legends.push({
            caption: "min score",
            color: c.VERDICT_NOK2,
        });
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
        legends: legends,
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

    const data = stackedBarsData(resultType, tasks, statistics, count);

    return (
        <StatisticsWrap>
            <StatisticsHeader>
                <StatisticsHeaderWrapper>
                    <StatisticsHeaderTitle>{c.STATISTICS_TITLE}</StatisticsHeaderTitle>
                    <StatisticsHeaderCaption>{c.STATISTICS_CAPTION}</StatisticsHeaderCaption>
                </StatisticsHeaderWrapper>
                <StatisticsLegends legends={data.legends}></StatisticsLegends>
            </StatisticsHeader>

            <StackedBarsStatistics data={data} rowsCount={rowsCount}/>
        </StatisticsWrap>
    )
};
export default Statistics;
