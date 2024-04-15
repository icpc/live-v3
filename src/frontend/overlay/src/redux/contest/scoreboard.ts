import _ from "lodash";
import { LegacyScoreboardRow, OptimismLevel } from "@shared/api";

const ActionTypes = {
    SCOREBOARD_SET: "SCOREBOARD_SET",
};

export type ScoreboardData = {
    rows: LegacyScoreboardRow[],
    ids: Record<LegacyScoreboardRow["teamId"], LegacyScoreboardRow>
};

export type ScoreboardState = {
    [key in OptimismLevel]: ScoreboardData
}

const initialState: ScoreboardState = {
    [OptimismLevel.normal]: {
        rows: [],
        ids: {}
    },
    [OptimismLevel.optimistic]: {
        rows: [],
        ids: {}
    },
    [OptimismLevel.pessimistic]: {
        rows: [],
        ids: {}
    }
};

export const setScoreboard = (scoreboardType, rows) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SCOREBOARD_SET,
            payload: {
                type: scoreboardType,
                rows: rows
            }
        });
    };
};

export function scoreboardReducer(state = initialState, action): ScoreboardState {
    switch (action.type) {
    case ActionTypes.SCOREBOARD_SET:
        return {
            ...state,
            [action.payload.type]: {
                rows: action.payload.rows,
                ids: _.keyBy(action.payload.rows, "teamId")
            }
        };
    default:
        return state;
    }
}
