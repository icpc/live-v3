import { handleScoreboardDiff } from "../../redux/contest/scoreboard";
import { OptimismLevel, ScoreboardDiff } from "@shared/api";

export const handleMessage = (optimism: OptimismLevel) => (dispatch, e) => {
    const diff = JSON.parse(e.data) as ScoreboardDiff;
    dispatch(handleScoreboardDiff({ optimism, diff }));
};
