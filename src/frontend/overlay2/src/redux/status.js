import { WEBSOCKETS } from "../consts";

const ActionTypes = {
    SET_WEBSOCKET_STATUS: "SET_WEBSOCKET_STATUS"
};

export const WebsocketStatus = Object.freeze({
    CONNECTING: 0,
    CONNECTED: 1,
    DISCONNECTED: -1
});

const initialState = {
    websockets: Object.fromEntries(Object.keys(WEBSOCKETS).map((key) => {
        return [key, undefined];
    })),
};

export const setWebsocketStatus = (socket, newStatus) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SET_WEBSOCKET_STATUS,
            payload: {
                [socket]: newStatus
            }
        });
    };
};

export function statusReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.SET_WEBSOCKET_STATUS:
        return {
            ...state,
            websockets: {
                ...state.websockets,
                ...action.payload
            },
        };
    default:
        return state;
    }
}
