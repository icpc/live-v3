import _ from "lodash";
import { RunInfo } from "@shared/api";

const ActionTypes = {
    QUEUE_ADD_RUN: "QUEUE_ADD_RUN",
    QUEUE_MODIFY_RUN: "QUEUE_MODIFY_RUN",
    QUEUE_REMOVE_RUN: "QUEUE_REMOVE_RUN",
    QUEUE_SET_FROM_SNAPSHOT: "QUEUE_SET_FROM_SNAPSHOT"
};


interface QueueState {
    queue: RunInfo[],
    totalQueueItems: number
}

const initialState: QueueState = {
    queue: [],
    totalQueueItems: 0,
};

export const addRun = (runData) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.QUEUE_ADD_RUN,
            payload: {
                newRun: runData
            }
        });
    };
};

export const modifyRun = (runData) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.QUEUE_MODIFY_RUN,
            payload: {
                runData
            }
        });
    };
};

export const removeRun = (runId) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.QUEUE_REMOVE_RUN,
            payload: {
                runId
            }
        });
    };
};

export const setFromSnapshot = (snapshot) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.QUEUE_SET_FROM_SNAPSHOT,
            payload: {
                runs: snapshot
            }
        });
    };
};

export function queueReducer(state = initialState, action): QueueState {
    switch (action.type) {
    case ActionTypes.QUEUE_ADD_RUN:
        return {
            ...state,
            queue: [
                action.payload.newRun,
                ...state.queue.filter((run) => run.id !== action.payload.newRun.id)
            ],
            totalQueueItems: state.totalQueueItems + 1
        };
    case ActionTypes.QUEUE_MODIFY_RUN:
        return {
            ...state,
            queue: state.queue.map((run) => run.id === action.payload.runData.id ?
                action.payload.runData :
                run)
        };
    case ActionTypes.QUEUE_REMOVE_RUN:
        return {
            ...state,
            queue: _.filter(state.queue, (run) => run.id !== action.payload.runId)
        };
    case ActionTypes.QUEUE_SET_FROM_SNAPSHOT:
        return {
            ...state,
            queue: _.reverse(action.payload.runs)
        };
    default:
        return state;
    }
}
