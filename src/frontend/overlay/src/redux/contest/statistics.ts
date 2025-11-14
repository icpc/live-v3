import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { SolutionsStatistic } from "@shared/api";
export interface StatisticsState {
    statistics?: SolutionsStatistic;
}

const initialState: StatisticsState = {
    statistics: null,
};

export const statisticsSlice = createSlice({
    name: "statistics",
    initialState,
    reducers: {
        setStatistics: (state, action: PayloadAction<SolutionsStatistic>) => {
            state.statistics = action.payload;
        },
    },
});

export const { setStatistics } = statisticsSlice.actions;

export default statisticsSlice.reducer;
