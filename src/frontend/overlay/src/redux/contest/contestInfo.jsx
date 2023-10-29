import _ from "lodash";
import c from "../../config";
import {getTextWidth} from "../../components/atoms/ShrinkingBox";

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
        _.forEach(action.payload.info.teams, (team) => getTextWidth(team.shortName, c.GLOBAL_DEFAULT_FONT));
        let sortedProblems = action.payload.info.problems.sort((a, b) => a.ordinal - b.ordinal);
        return {
            ...state,
            info: {
                ...action.payload.info,
                problems: sortedProblems,
                teamsId: _.keyBy(action.payload.info.teams, "id"),
                problemsId: _.keyBy(sortedProblems, "id")
            }
        };
    default:
        return state;
    }
}
