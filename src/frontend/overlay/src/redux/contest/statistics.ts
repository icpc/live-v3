import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { ProblemSolutionStatistic, SolutionsStatistic } from "@shared/api";
export interface StatisticsState {
    statistics?: ProblemSolutionStatistic[];
}

const initialState: StatisticsState = {
    statistics: null
};


export const statisticsSlice = createSlice({
    name: "statistics",
    initialState,
    reducers: {
        setStatistics: (state, action: PayloadAction<SolutionsStatistic>) => {
            state.statistics = action.payload.stats;
        }
    }
});

export const { setStatistics } = statisticsSlice.actions;

export default statisticsSlice.reducer;
