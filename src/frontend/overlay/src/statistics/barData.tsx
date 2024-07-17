import { StatisticsData } from "@/components/molecules/statistics/types";
import { getIOIColor } from "@/utils/statusInfo";
import c from "../config";
import { ProblemInfo, SolutionsStatistic } from "@shared/api";

export const stackedBarsData = (tasks: ProblemInfo[], statistics: SolutionsStatistic,): StatisticsData => {
    if (!tasks || !statistics || !statistics.teamsCount || tasks.length != statistics.stats.length) {
        return {
            legend: [],
            data: [],
        };
    }
    const count = statistics.teamsCount;
    const legend = [];
    const bars = [];
    if (statistics.type == SolutionsStatistic.Type.ICPC) {
        legend.push({
            caption: "solved",
            color: c.VERDICT_OK,
        });
        legend.push({
            caption: "pending",
            color: c.VERDICT_UNKNOWN,
        });
        legend.push({
            caption: "incorrect",
            color: c.VERDICT_NOK,
        });
        bars.push(...statistics.stats?.map(({ success, pending, wrong }, index) => ({
            name: tasks[index].letter,
            color: tasks[index].color,
            values: [
                {
                    color: c.VERDICT_OK,
                    caption: success ? success.toString() : "",
                    value: count ? success / count : 0.0,
                },
                {
                    color: c.VERDICT_UNKNOWN,
                    caption: pending ? pending.toString() : "",
                    value: count ? pending / count : 0.0,
                },
                {
                    color: c.VERDICT_NOK,
                    caption: wrong ? wrong.toString() : "",
                    value: count ? wrong / count : 0.0,
                },
            ]
        })) ?? []);
    } else if (statistics.type == SolutionsStatistic.Type.IOI) {
        legend.push({
            caption: "max score",
            color: c.VERDICT_OK2,
        });
        legend.push({
            caption: "min score",
            color: c.VERDICT_NOK2,
        });
        bars.push(...statistics.stats?.map(({ result }, index) => ({
            name: tasks[index].letter,
            color: tasks[index].color,
            values: result.map(({ count: rCount, score }) => ({
                color: getIOIColor(score, tasks[index]?.minScore, tasks[index]?.maxScore),
                value: count ? rCount / count : 0.0,
            })),
        })) ?? []);
    }
    return {
        legend,
        data: bars
    };
};
