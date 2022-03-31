import _ from "lodash";

const ActionTypes = {
    ADD_MESSAGE: "ADD_MESSAGE",
    REMOVE_MESSAGE: "REMOVE_MESSAGE",
    SET_MESSAGES: "SET_MESSAGES"
};

const initialState = {
    messages: []
};

export const addMessage = (messageData) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.ADD_MESSAGE,
            payload: {
                newMessage: messageData
            }
        });
    };
};

export const removeMessage = (messageId) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.REMOVE_MESSAGE,
            payload: {
                messageId
            }
        });
    };
};

export const setMessages = (messages) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SET_MESSAGES,
            payload: {
                messages
            }
        });
    };
};

export function tickerReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.ADD_MESSAGE:
        return {
            messages: [
                ...state.messages,
                action.payload.newMessage
            ]
        };
    case ActionTypes.REMOVE_MESSAGE:
        return {
            messages: _.filter(state.messages, (message) => message.id !== action.payload.messageId)
        };
    case ActionTypes.SET_MESSAGES:
        return {
            messages: action.payload.messages
        };
    default:
        return state;
    }
}
