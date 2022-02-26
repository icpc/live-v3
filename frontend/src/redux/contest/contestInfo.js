import _ from "lodash";

const ActionTypes = {
    CONTEST_INFO_SET: "CONTEST_INFO_SET",
};

const initialState = {
    info: undefined
};

export const setInfo = (info) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.CONTEST_INFO_SET,
            payload: {
                info
            }
        });
    };
};

export function contestInfoReducer(state = initialState, action) {
    switch (action.type) {
    case ActionTypes.CONTEST_INFO_SET:
        return {
            ...state,
            info: {
                ...action.payload.info,
                teamsId: _.keyBy(action.payload.info.teams, "id"),
            }
        };
    default:
        return state;
    }
}
