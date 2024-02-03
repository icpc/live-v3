import { setStatistics } from "../../redux/contest/statistics";
import { SolutionsStatistic } from "@shared/api";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data) as SolutionsStatistic;
    dispatch(setStatistics(message));
};
