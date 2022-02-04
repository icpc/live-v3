import { DateTime } from "luxon";

const ActionTypes = {
    PUSH_LOG: "PUSH_LOG"
};

const initialState = {
    log: []
};

export const pushLog = (text) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.PUSH_LOG,
            payload: {
                text,
                timestamp: DateTime.now().toLocaleString(DateTime.TIME_24_WITH_SECONDS)
            }
        });
    };
};

export function debugReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.PUSH_LOG:
        return {
            ...state,
            log: [
                ...state.log,
                action.payload
            ]
        };
    default:
        return state;
    }
}
