import _ from "lodash";
import { TickerMessage, TickerPart } from "@shared/api";
import type { RootState } from "./store";

enum ActionTypes {
    ADD_MESSAGE = "TICKER_ADD_MESSAGE",
    REMOVE_MESSAGE = "TICKER_REMOVE_MESSAGE",
    SET_MESSAGES = "TICKER_SET_MESSAGES",
    SET_CUR_DISPLAYING = "TICKER_SET_CUR_DISPLAYING",
    CLEAR_CUR_DISPLAYING = "TICKER_CLEAR_CUR_DISPLAYING",
    START_DISPLAYING = "TICKER_START_DISPLAYING",
    STOP_DISPLAYING = "TICKER_STOP_DISPLAYING",
}

type AddMessageAction = {
    type: ActionTypes.ADD_MESSAGE;
    payload: { newMessage: TickerMessage };
};

type RemoveMessageAction = {
    type: ActionTypes.REMOVE_MESSAGE;
    payload: { messageId: TickerMessage["id"] };
};

type SetMessagesAction = {
    type: ActionTypes.SET_MESSAGES;
    payload: { messages: TickerMessage[] };
};

type SetCurDisplayingAction = {
    type: ActionTypes.SET_CUR_DISPLAYING;
    payload: {
        part: TickerPart;
        ind: number;
        message: TickerMessage;
        timeout: ReturnType<typeof setTimeout>;
        isFirst: boolean;
    };
};

type ClearCurDisplayingAction = {
    type: ActionTypes.CLEAR_CUR_DISPLAYING;
    payload: { part: TickerPart };
};

type StartDisplayingAction = { type: ActionTypes.START_DISPLAYING };

type StopDisplayingAction = { type: ActionTypes.STOP_DISPLAYING };

type TickerAction =
    | AddMessageAction
    | RemoveMessageAction
    | SetMessagesAction
    | SetCurDisplayingAction
    | ClearCurDisplayingAction
    | StartDisplayingAction
    | StopDisplayingAction;

type TickerThunk = (
    dispatch: TickerDispatch,
    getState: GetState,
) => Promise<void>;

type TickerDispatch = (action: TickerAction | TickerThunk) => void;

type GetState = () => RootState;

const TICKER_PARTS: readonly TickerPart[] = Object.freeze([
    TickerPart.long,
    TickerPart.short,
]);

type TickerPartState = {
    messages: TickerMessage[];
    curDisplaying: TickerMessage | undefined;
    curDisplayingIndex: number | undefined;
    curTimeout: ReturnType<typeof setTimeout> | undefined;
    isFirst: boolean;
};

type TickerState = {
    tickers: Record<TickerPart, TickerPartState>;
    isLoaded: boolean;
    isDisplaying: boolean;
};

const byPart = (
    value: (part: TickerPart) => TickerPartState,
): Record<TickerPart, TickerPartState> =>
    Object.fromEntries(
        TICKER_PARTS.map((part) => [part, value(part)]),
    ) as Record<TickerPart, TickerPartState>;

const defaultTickerPartBody: TickerPartState = {
    messages: [],
    curDisplaying: undefined,
    curDisplayingIndex: undefined,
    curTimeout: undefined,
    isFirst: true,
};

const initialState: TickerState = {
    tickers: byPart(() => defaultTickerPartBody),
    isLoaded: false,
    isDisplaying: false,
};

export const startScrolling = () => {
    return async (dispatch: TickerDispatch, getState: GetState) => {
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
    return async (dispatch: TickerDispatch, getState: GetState) => {
        const state = getState();
        for (const part of TICKER_PARTS) {
            clearTimeout(state.ticker.tickers[part].curTimeout);
        }
        dispatch({
            type: ActionTypes.STOP_DISPLAYING,
        });
    };
};

export const advanceScrolling = (part: TickerPart, add = 1, isFirst = true) => {
    return async (dispatch: TickerDispatch, getState: GetState) => {
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
                type: ActionTypes.CLEAR_CUR_DISPLAYING,
                payload: { part },
            });
        }
    };
};

export const addMessage = (messageData: TickerMessage) => {
    return async (dispatch: TickerDispatch, getState: GetState) => {
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

export const removeMessage = (messageId: TickerMessage["id"]) => {
    return async (dispatch: TickerDispatch, getState: GetState) => {
        const { ticker } = getState();
        const displayingPart = TICKER_PARTS.find(
            (part) => ticker.tickers[part].curDisplaying?.id === messageId,
        );
        dispatch({
            type: ActionTypes.REMOVE_MESSAGE,
            payload: {
                messageId,
            },
        });
        // The message that was on screen is gone, so move on to whatever took its place.
        if (displayingPart !== undefined) {
            dispatch(advanceScrolling(displayingPart, 0, false));
        }
    };
};

export const setMessages = (messages: TickerMessage[]) => {
    return async (dispatch: TickerDispatch, getState: GetState) => {
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

export function tickerReducer(
    state = initialState,
    action: TickerAction,
): TickerState {
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
            };
        case ActionTypes.REMOVE_MESSAGE:
            return {
                ...state,
                tickers: byPart((part) => ({
                    ...state.tickers[part],
                    messages: _.filter(
                        state.tickers[part].messages,
                        (message) => message.id !== action.payload.messageId,
                    ),
                })),
            };
        case ActionTypes.SET_MESSAGES:
            return {
                ...state,
                tickers: byPart((part) => ({
                    ...defaultTickerPartBody,
                    messages: _.filter(action.payload.messages, ["part", part]),
                })),
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
        case ActionTypes.CLEAR_CUR_DISPLAYING:
            return {
                ...state,
                tickers: {
                    ...state.tickers,
                    [action.payload.part]: {
                        ...state.tickers[action.payload.part],
                        curDisplaying: undefined,
                        curDisplayingIndex: undefined,
                        curTimeout: undefined,
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
                tickers: byPart((part) => ({
                    ...state.tickers[part],
                    curDisplaying: undefined,
                    curDisplayingIndex: undefined,
                    curTimeout: undefined,
                    isFirst: true,
                })),
            };
        default:
            return state;
    }
}
