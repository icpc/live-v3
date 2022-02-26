import _ from "lodash";

const ActionTypes = {
    SHOW_WIDGET: "SHOW_WIDGET",
    HIDE_WIDGET: "HIDE_WIDGET",
    SET_WIDGETS: "SET_WIDGETS"
};

const initialState = {
    widgets: {}
};

export const showWidget = (widgetData) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SHOW_WIDGET,
            payload: {
                newWidget: widgetData
            }
        });
    };
};

export const hideWidget = (widgetId) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.HIDE_WIDGET,
            payload: {
                widgetId
            }
        });
    };
};

export const setWidgets = (widgets) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.SET_WIDGETS,
            payload: {
                widgets
            }
        });
    };
};

export function widgetsReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.SHOW_WIDGET:
        return {
            widgets: {
                ...state.widgets,
                [action.payload.newWidget.widgetId]: action.payload.newWidget
            }
        };
    case ActionTypes.HIDE_WIDGET:
        return {
            widgets: _.omit(state.widgets, action.payload.widgetId)
        };
    case ActionTypes.SET_WIDGETS:
        return {
            widgets: Object.fromEntries(action.payload.widgets.map((widget) => [widget.widgetId, widget]))
        };
    default:
        return state;
    }
}
