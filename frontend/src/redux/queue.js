import _ from "lodash";

const ActionTypes = {
    ADD_RUN: "ADD_RUN",
    MODIFY_RUN: "MODIFY_RUN",
    REMOVE_RUN: "REMOVE_RUN",
};

const initialState = {
    queue: [],
    breakingNews: undefined
};

export const addRun = (runData) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.ADD_RUN,
            payload: {
                newRun: runData
            }
        });
    };
};

export const modifyRun = (runData) => {
    return async dispatch => {
        console.log(runData);
        dispatch({
            type: ActionTypes.MODIFY_RUN,
            payload: {
                runData
            }
        });
    };
};

export const removeRun = (runId) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.REMOVE_RUN,
            payload: {
                runId
            }
        });
    };
};

export function queueReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.ADD_RUN:
        return {
            queue: [
                action.payload.newRun,
                ...state.queue
            ]
        };
    case ActionTypes.MODIFY_RUN:
        return {
            queue: state.queue.map((run) => run.id === action.payload.runData.id ?
                action.payload.runData :
                run)
        };
    case ActionTypes.REMOVE_RUN:
        return {
            queue: _.differenceBy(state.queue, [{ id: action.payload.runId }], "id")
        };
    default:
        return state;
    }
}
