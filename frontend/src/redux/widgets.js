const ActionTypes = {
    SHOW_WIDGET: "SHOW_WIDGET",
    HIDE_WIDGET: "HIDE_WIDGET",
};

const initialState = {
    widgets: []
};

export function widgetsReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.STORE_SETTINGS:
        return {
            loaded: true,
            settings: action.payload,
        };
    default:
        return state;
    }
}
