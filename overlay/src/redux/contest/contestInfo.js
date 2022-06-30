import _ from "lodash";
import { getTextWidth } from "../../components/atoms/ContestCells";
import { GLOBAL_DEFAULT_FONT } from "../../config";

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
        _.forEach(action.payload.info.teams, (team) => getTextWidth(team.shortName, GLOBAL_DEFAULT_FONT));
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
