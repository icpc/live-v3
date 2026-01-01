import _ from "lodash";
import {
    Award,
    OptimismLevel,
    ScoreboardDiff,
    ScoreboardRow,
    TeamId,
} from "@shared/api";
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export type ScoreboardData = {
    ids: Record<TeamId, ScoreboardRow>; // maybe stable?
    idAwards: Record<TeamId, undefined | Exclude<Award, "teams">[]>;
    order: TeamId[];
    orderById: Record<TeamId, number>;
    rankById: Record<TeamId, number>;
    ranks: number[];
    awards: Award[];
};

export type ScoreboardState = {
    [key in OptimismLevel]: ScoreboardData;
};

const initialState: ScoreboardState = Object.fromEntries(
    Object.keys(OptimismLevel).map((key: OptimismLevel) => [
        key,
        {
            ids: {},
            idAwards: {},
            order: [],
            ranks: [],
            awards: [],
            orderById: {},
            rankById: {},
        },
    ]),
) as ScoreboardState;

const scoreboardSlice = createSlice({
    name: "scoreboard",
    initialState,
    reducers: {
        handleScoreboardDiff(
            state,
            action: PayloadAction<{
                optimism: OptimismLevel;
                diff: ScoreboardDiff;
            }>,
        ) {
            const { optimism, diff } = action.payload;
            const s = state[optimism];

            if (!_.isEqual(s.awards, diff.awards)) {
                s.awards = diff.awards;
            }
            if (!_.isEqual(s.order, diff.order)) {
                s.order = diff.order;
            }
            if (!_.isEqual(s.ranks, diff.ranks)) {
                s.ranks = diff.ranks;
            }
            const nextIdAwards = {};
            for (const award of diff.awards) {
                for (const teamId of award.teams) {
                    if (!nextIdAwards[teamId]) nextIdAwards[teamId] = [];
                    nextIdAwards[teamId].push({ ...award, teams: undefined });
                }
            }

            for (const [id, newData] of Object.entries(diff.rows)) {
                if (!_.isEqual(s.ids[id], newData)) {
                    s.ids[id] = newData;
                }
            }

            const nextOrderById = Object.fromEntries(
                diff.order.map((teamId, index) => [teamId, index]),
            );
            if (!_.isEqual(s.orderById, nextOrderById)) {
                s.orderById = nextOrderById;
            }

            const nextRankById = Object.fromEntries(_.zip(diff.order, diff.ranks));
            if (!_.isEqual(s.rankById, nextRankById)) {
                s.rankById = nextRankById;
            }

            Object.keys(s.idAwards).forEach((id) => {
                if (!(id in nextIdAwards)) delete s.idAwards[id];
            });
            for (const id in nextIdAwards) {
                if (!_.isEqual(s.idAwards[id], nextIdAwards[id])) {
                    s.idAwards[id] = nextIdAwards[id];
                }
            }
        },
    },
});

export const { handleScoreboardDiff } = scoreboardSlice.actions;

export default scoreboardSlice.reducer;
