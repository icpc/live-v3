const ActionTypes = {
    SHOW_WIDGET: "SHOW_WIDGET",
    HIDE_WIDGET: "HIDE_WIDGET",
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


const omitKey = (obj, key) => {
    let { [key]: omit, ...res } = obj;
    return res;
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
            widgets: omitKey(state.widgets, action.payload.widgetId)
        };
    default:
        return state;
    }
}
