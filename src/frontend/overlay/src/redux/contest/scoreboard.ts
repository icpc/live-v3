import _ from "lodash";
import { Award, OptimismLevel, ScoreboardDiff, ScoreboardRow, TeamId } from "@shared/api";
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export type ScoreboardData = {
    ids: Record<TeamId, ScoreboardRow>, // maybe stable?
    idAwards: Record<TeamId, undefined | Exclude<Award, "teams">[]>,
    order: TeamId[],
    orderById: Record<TeamId, number>,
    rankById: Record<TeamId, number>,
    ranks: number[],
    awards: Award[]
};

export type ScoreboardState = {
    [key in OptimismLevel]: ScoreboardData
}

const initialState: ScoreboardState = Object.fromEntries(
    Object.keys(OptimismLevel).map((key) => [key, {
        ids: {},
        idAwards: {},
        order: [],
        ranks: [],
        awards: []
    }])
) as ScoreboardState;

const scoreboardSlice = createSlice({
    name: "scoreboard",
    initialState,
    reducers: {
        handleScoreboardDiff(state, action: PayloadAction<{ optimism: OptimismLevel, diff: ScoreboardDiff }>) {
            const { optimism, diff } = action.payload;
            state[optimism].awards = diff.awards;
            state[optimism].order = diff.order;
            state[optimism].ranks = diff.ranks;
            for (const [id, newData] of Object.entries(diff.rows)) {
                state[optimism].ids[id] = newData;
            }
            state[optimism].orderById = Object.fromEntries(
                diff.order.map((teamId, index) => [teamId, index])
            );
            state[optimism].rankById = Object.fromEntries(_.zip(diff.order, diff.ranks));
            state[optimism].idAwards = {};
            for (const award of diff.awards) {
                for (const teamId of award.teams) {
                    if(state[optimism].idAwards[teamId] === undefined) {
                        state[optimism].idAwards[teamId] = [];
                    }
                    state[optimism].idAwards[teamId].push({ ...award, teams: undefined });
                }
            }
        }
    }
});

export const { handleScoreboardDiff } = scoreboardSlice.actions;

export default scoreboardSlice.reducer;
