const ActionTypes = {
    SET_WEBSOCKET_STATUS: "SET_WEBSOCKET_STATUS"
};

export const WebsocketStatus = Object.freeze({
    CONNECTING: 0,
    CONNECTED: 1,
    DISCONNECTED: -1
});

const initialState = {
    websocketStatus: undefined,
};

export const setWebsocketStatus = (newStatus) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SET_WEBSOCKET_STATUS,
            payload: {
                websocketStatus: newStatus
            }
        });
    };
};

export function statusReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.SET_WEBSOCKET_STATUS:
        return {
            ...state,
            websocketStatus: action.payload.websocketStatus
        };
    default:
        return state;
    }
}
