const ActionTypes = {
    STATISTICS_SET: "STATISTICS_SET",
};

const initialState = {
    statistics: undefined
};

export const setStatistics = (statistics) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.STATISTICS_SET,
            payload: {
                statistics
            }
        });
    };
};

export function statisticsReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.STATISTICS_SET:
        return {
            ...state,
            statistics: action.payload.statistics
        };
    default:
        return state;
    }
}
