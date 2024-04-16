import _ from "lodash";
import c from "../../config";
import { getTextWidth } from "../../components/atoms/ShrinkingBox";
import { ContestInfo, ProblemInfo, TeamInfo } from "@shared/api";

const ActionTypes = {
    CONTEST_INFO_SET: "CONTEST_INFO_SET",
};

type ContestState = {
    info: (ContestInfo & {
        teamsId: Record<TeamInfo["id"], TeamInfo>,
        problemsId: Record<ProblemInfo["id"], ProblemInfo>,
    }) | undefined
}

const initialState: ContestState = {
    info: undefined
};

export const setInfo = (info: ContestInfo) => {
    return async dispatch => {
        dispatch({
            type: ActionTypes.CONTEST_INFO_SET,
            payload: {
                info
            }
        });
    };
};

export function contestInfoReducer(state = initialState, action): ContestState {
    switch (action.type) {
    case ActionTypes.CONTEST_INFO_SET:
        _.forEach(action.payload.info.teams, (team) => getTextWidth(team.shortName, c.GLOBAL_DEFAULT_FONT));
        const sortedProblems = action.payload.info.problems.sort((a, b) => a.ordinal - b.ordinal).filter( a => !a.isHidden);
        return {
            ...state,
            info: {
                ...action.payload.info,
                problems: sortedProblems,
                teamsId: _.keyBy(action.payload.info.teams, "id"),
                problemsId: _.keyBy(action.payload.info.problems, "id")
            }
        };
    default:
        return state;
    }
}
