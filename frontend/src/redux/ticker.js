import _ from "lodash";

const ActionTypes = {
    ADD_MESSAGE: "ADD_MESSAGE",
    REMOVE_MESSAGE: "REMOVE_MESSAGE",
    SET_MESSAGES: "SET_MESSAGES",
    SET_CUR_DISPLAYING: "SET_CUR_DISPLAYING",
};

const TICKER_PARTS = Object.freeze(["long", "short"]);
//     messages: [],
//     curDisplaying: undefined,
//     curDisplayingIndex: 0,
//     curTimeout: 0
const defaultTickerBody = {
    messages: [],
    curDisplaying: undefined,
    curDisplayingIndex: 0,
    curTimeout: undefined,
    isFirst: true
};
const initialState = {
    tickers: Object.fromEntries(TICKER_PARTS.map((part) => (
        [part, defaultTickerBody]
    ))),
    messages: {},
    isLoaded: false
};

export const startScrolling = () => {
    return async (dispatch, getState) => {
        const state = getState();
        for (const part of TICKER_PARTS) {
            await advanceScrolling(part, 1)(dispatch, () => state);
        }
    };
};

export const stopScrolling = () => {
    return async (dispatch, getState) => {
        const state = getState();
        for (const part of TICKER_PARTS) {
            clearTimeout(state.ticker.tickers[part].curTimeout);
        }
    };
};

export const advanceScrolling = (part, add = 1) => {
    return async (dispatch, getState) => {
        console.log("!advanceScrolling");
        const state = getState();
        const curDisplayingIndex = state.ticker.tickers[part].curDisplayingIndex;
        console.log(curDisplayingIndex);
        const messages = state.ticker.tickers[part].messages;
        console.log(messages);
        const newCurDisplayIndex = (curDisplayingIndex + add) % messages.length;
        console.log(newCurDisplayIndex);
        const newMessage = messages[newCurDisplayIndex];
        console.log(newMessage);
        const timeout = newMessage !== undefined ? setTimeout(() => {
            dispatch(advanceScrolling(part, 1));
        }, newMessage.periodMs) : undefined;
        dispatch({
            type: ActionTypes.SET_CUR_DISPLAYING,
            payload: {
                part,
                ind: newCurDisplayIndex,
                message: newMessage,
                timeout
            }
        });
    };
};

export const addMessage = (messageData) => {
    return async (dispatch, getState) => {
        const state = getState();
        const part = messageData.part;
        const ticker = state.ticker.tickers[part];
        dispatch({
            type: ActionTypes.ADD_MESSAGE,
            payload: {
                newMessage: messageData
            }
        });
        if(ticker.curTimeout === undefined) {
            dispatch(advanceScrolling(part, 0));
        }
    };
};

export const removeMessage = (messageId) => {
    return async (dispatch, getState) => {
        const { ticker } = getState();
        const part = ticker.messages[messageId].part;
        const curMessage = ticker.tickers[part].curDisplaying;
        if (curMessage && curMessage.id === messageId) {
            dispatch(advanceScrolling(part, 1));
        }
        dispatch({
            type: ActionTypes.REMOVE_MESSAGE,
            payload: {
                part,
                messageId
            }
        });
    };
};

export const setMessages = (messages) => {
    return async dispatch => {
        dispatch(stopScrolling());
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
            ...state,
            tickers: {
                ...state.tickers,
                [action.payload.newMessage.part]: {
                    ...state.tickers[action.payload.newMessage.part],
                    messages: [
                        ...state.tickers[action.payload.newMessage.part].messages,
                        action.payload.newMessage
                    ],
                    curDisplaying: state.tickers[action.payload.newMessage.part].curDisplaying ?? action.payload.newMessage
                }
            },
            messages: {
                ...state.messages,
                [action.payload.newMessage.id]: action.payload.newMessage
            }
        };
    case ActionTypes.REMOVE_MESSAGE:
        return {
            ...state,
            tickers: {
                ...state.tickers,
                [action.payload.part]: {
                    ...state.tickers[action.payload.part],
                    messages: _.filter(state.tickers[action.payload.part].messages, (message) => message.id !== action.payload.messageId)
                }
            },
            messages: _.omit(state.messages, action.payload.messageId)
        };
    case ActionTypes.SET_MESSAGES:
        return {
            ...state,
            tickers: Object.fromEntries(TICKER_PARTS.map((part) => (
                [part, {
                    ...defaultTickerBody,
                    messages: _.filter(action.payload.messages, ["part", part]),
                    curDisplaying: _.find(action.payload.messages, ["part", part])
                }]
            ))),
            messages: _.keyBy(action.payload.messages, "id"),
            isLoaded: true
        };
    case ActionTypes.SET_CUR_DISPLAYING:
        return {
            ...state,
            tickers: {
                ...state.tickers,
                [action.payload.part]: {
                    ...state.tickers[action.payload.part],
                    curDisplaying: action.payload.message,
                    curDisplayingIndex: action.payload.ind,
                    curTimeout: action.payload.timeout,
                    isFirst: false
                }
            }
        };
    default:
        return state;
    }
}

