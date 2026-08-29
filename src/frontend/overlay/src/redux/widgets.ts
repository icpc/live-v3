import { OrderedWidget, Widget } from "@shared/api";
import _ from "lodash";

enum ActionTypes {
    SHOW_WIDGET = "SHOW_WIDGET",
    HIDE_WIDGET = "HIDE_WIDGET",
    SET_WIDGETS = "SET_WIDGETS",
}

type ShowWidgetAction = {
    type: ActionTypes.SHOW_WIDGET;
    payload: { newWidget: OrderedWidget };
};

type HideWidgetAction = {
    type: ActionTypes.HIDE_WIDGET;
    payload: { widgetId: Widget["widgetId"] };
};

type SetWidgetsAction = {
    type: ActionTypes.SET_WIDGETS;
    payload: { widgets: OrderedWidget[] };
};

type WidgetsAction = ShowWidgetAction | HideWidgetAction | SetWidgetsAction;

type WidgetsDispatch = (action: WidgetsAction) => void;

type WidgetsState = {
    widgetsWithOrder: OrderedWidget[];
    widgets: Record<Widget["widgetId"], Widget>;
};

const stateOf = (widgetsWithOrder: OrderedWidget[]): WidgetsState => ({
    widgetsWithOrder,
    widgets: Object.fromEntries(
        _.sortBy(widgetsWithOrder, "showOrder").map((it) => [
            it.widget.widgetId,
            it.widget,
        ]),
    ),
});

const filterOutId = (
    widgetsWithOrder: OrderedWidget[],
    widgetId: Widget["widgetId"],
) => widgetsWithOrder.filter((it) => it.widget.widgetId !== widgetId);

const initialState: WidgetsState = stateOf([]);

export const showWidget = (widgetData: OrderedWidget) => {
    return async (dispatch: WidgetsDispatch) => {
        dispatch({
            type: ActionTypes.SHOW_WIDGET,
            payload: {
                newWidget: widgetData,
            },
        });
    };
};

export const hideWidget = (widgetId: Widget["widgetId"]) => {
    return async (dispatch: WidgetsDispatch) => {
        dispatch({
            type: ActionTypes.HIDE_WIDGET,
            payload: {
                widgetId,
            },
        });
    };
};

export const setWidgets = (widgets: OrderedWidget[]) => {
    return async (dispatch: WidgetsDispatch) => {
        dispatch({
            type: ActionTypes.SET_WIDGETS,
            payload: {
                widgets,
            },
        });
    };
};

export function widgetsReducer(
    state = initialState,
    action: WidgetsAction,
): WidgetsState {
    switch (action.type) {
        case ActionTypes.SHOW_WIDGET: {
            const { newWidget } = action.payload;
            return stateOf([
                ...filterOutId(
                    state.widgetsWithOrder,
                    newWidget.widget.widgetId,
                ),
                newWidget,
            ]);
        }
        case ActionTypes.HIDE_WIDGET:
            return stateOf(
                filterOutId(state.widgetsWithOrder, action.payload.widgetId),
            );
        case ActionTypes.SET_WIDGETS:
            return stateOf(action.payload.widgets);
        default:
            return state;
    }
}
