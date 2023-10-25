import {StatisticsData} from "../components/molecules/statistics/types";
import {getTeamTaskColor} from "../utils/statusInfo"
import c from "../config"

export const stackedBarsData = (resultType: string, tasks: any[], statistics: any[], count: number): StatisticsData => {
    if (!tasks || !statistics || !count) {
        return {
            legend: [],
            data: [],
        };
    }

    const legend = [];
    const bars = [];
    if (resultType === "ICPC") {
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
        bars.push(...statistics?.map(({result, success, pending, wrong}, index) => ({
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
        })));
    } else if (resultType === "IOI") {
        legend.push({
            caption: "max score",
            color: c.VERDICT_OK2,
        });
        legend.push({
            caption: "min score",
            color: c.VERDICT_NOK2,
        });
        bars.push(...statistics?.map(({result, success, pending, wrong}, index) => ({
            name: tasks[index].letter,
            color: tasks[index].color,
            values: result.map(({count: rCount, score}) => ({
                color: getTeamTaskColor(score, tasks[index]?.minScore, tasks[index]?.maxScore),
                value: count ? rCount / count : 0.0,
            })),
        })));
    }
    return {
        legend,
        data: bars
    };
}
