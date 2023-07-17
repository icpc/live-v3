import _ from "lodash";
import { SCOREBOARD_TYPES } from "../../consts";

const ActionTypes = {
    SCOREBOARD_SET: "SCOREBOARD_SET",
};

const initialState = {
    [SCOREBOARD_TYPES.normal]: {
        rows: [],
        ids: {}
    },
    [SCOREBOARD_TYPES.optimistic]: {
        rows: [],
        ids: {}
    },
    [SCOREBOARD_TYPES.pessimistic]: {
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

export function scoreboardReducer(state = initialState, action) {
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
