import _ from "lodash";

const ActionTypes = {
    ADD_MESSAGE: "TICKER_ADD_MESSAGE",
    REMOVE_MESSAGE: "TICKER_REMOVE_MESSAGE",
    SET_MESSAGES: "TICKER_SET_MESSAGES",
    SET_CUR_DISPLAYING: "TICKER_SET_CUR_DISPLAYING",
    START_DISPLAYING: "TICKER_START_DISPLAYING",
    STOP_DISPLAYING: "TICKER_STOP_DISPLAYING",
};

const TICKER_PARTS = Object.freeze(["long", "short"]);
const defaultTickerBody = {
    messages: [],
    curDisplaying: undefined,
    curDisplayingIndex: undefined,
    curTimeout: undefined,
    isFirst: true,
};
const initialState = {
    tickers: Object.fromEntries(
        TICKER_PARTS.map((part) => [part, defaultTickerBody]),
    ),
    messages: {},
    isLoaded: false,
    isDisplaying: false,
};

export const startScrolling = () => {
    return async (dispatch, getState) => {
        dispatch({
            type: ActionTypes.START_DISPLAYING,
        });
        const state = getState();
        for (const part of TICKER_PARTS) {
            await advanceScrolling(part, 0, true)(dispatch, () => state);
        }
    };
};

export const stopScrolling = () => {
    return async (dispatch, getState) => {
        const state = getState();
        for (const part of TICKER_PARTS) {
            clearTimeout(state.ticker.tickers[part].curTimeout);
        }
        dispatch({
            type: ActionTypes.STOP_DISPLAYING,
        });
    };
};

export const advanceScrolling = (part, add = 1, isFirst = true) => {
    return async (dispatch, getState) => {
        const state = getState();
        const curDisplayingIndex =
            state.ticker.tickers[part].curDisplayingIndex ?? 0;
        const messages = state.ticker.tickers[part].messages;
        const newCurDisplayIndex = (curDisplayingIndex + add) % messages.length;
        const newMessage = messages[newCurDisplayIndex];
        if (newMessage !== undefined) {
            clearTimeout(state.ticker.tickers[part].curTimeout);
            const timeout = setTimeout(() => {
                dispatch(advanceScrolling(part, 1, false));
            }, newMessage.periodMs);
            dispatch({
                type: ActionTypes.SET_CUR_DISPLAYING,
                payload: {
                    part,
                    ind: newCurDisplayIndex,
                    message: newMessage,
                    timeout,
                    isFirst,
                },
            });
        } else {
            dispatch({
                type: ActionTypes.SET_CUR_DISPLAYING,
                payload: {
                    part,
                    ind: undefined,
                    message: undefined,
                    timeout: undefined,
                },
            });
        }
    };
};

export const addMessage = (messageData) => {
    return async (dispatch, getState) => {
        const { ticker } = getState();
        const part = messageData.part;
        dispatch({
            type: ActionTypes.ADD_MESSAGE,
            payload: {
                newMessage: messageData,
            },
        });
        if (
            ticker.isDisplaying &&
            ticker.tickers[part].curTimeout === undefined
        ) {
            dispatch(advanceScrolling(part, 0, false));
        }
    };
};

export const removeMessage = (messageId) => {
    return async (dispatch, getState) => {
        const { ticker } = getState();
        const part = ticker.messages[messageId].part;
        const curMessage = ticker.tickers[part].curDisplaying;
        dispatch({
            type: ActionTypes.REMOVE_MESSAGE,
            payload: {
                part,
                messageId,
            },
        });
        if (curMessage && curMessage.id === messageId) {
            dispatch(advanceScrolling(part, 0, false));
        }
    };
};

export const setMessages = (messages) => {
    return async (dispatch, getState) => {
        const {
            ticker: { isDisplaying },
        } = getState();
        dispatch(stopScrolling());
        dispatch({
            type: ActionTypes.SET_MESSAGES,
            payload: {
                messages,
            },
        });
        if (isDisplaying) {
            dispatch(startScrolling());
        }
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
                            ...state.tickers[action.payload.newMessage.part]
                                .messages,
                            action.payload.newMessage,
                        ],
                    },
                },
                messages: {
                    ...state.messages,
                    [action.payload.newMessage.id]: action.payload.newMessage,
                },
            };
        case ActionTypes.REMOVE_MESSAGE:
            return {
                ...state,
                tickers: {
                    ...state.tickers,
                    [action.payload.part]: {
                        ...state.tickers[action.payload.part],
                        messages: _.filter(
                            state.tickers[action.payload.part].messages,
                            (message) =>
                                message.id !== action.payload.messageId,
                        ),
                    },
                },
                messages: _.omit(state.messages, action.payload.messageId),
            };
        case ActionTypes.SET_MESSAGES:
            return {
                ...state,
                tickers: Object.fromEntries(
                    TICKER_PARTS.map((part) => [
                        part,
                        {
                            ...defaultTickerBody,
                            messages: _.filter(action.payload.messages, [
                                "part",
                                part,
                            ]),
                        },
                    ]),
                ),
                messages: _.keyBy(action.payload.messages, "id"),
                isLoaded: true,
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
                        isFirst: action.payload.isFirst,
                    },
                },
            };
        case ActionTypes.START_DISPLAYING:
            return {
                ...state,
                isDisplaying: true,
            };
        case ActionTypes.STOP_DISPLAYING:
            return {
                ...state,
                isDisplaying: false,
                tickers: Object.fromEntries(
                    Object.entries(state.tickers).map(([part, ticker]) => [
                        part,
                        {
                            ...ticker,
                            curDisplaying: undefined,
                            curDisplayingIndex: undefined,
                            curTimeout: undefined,
                            isFirst: true,
                        },
                    ]),
                ),
            };
        default:
            return state;
    }
}
