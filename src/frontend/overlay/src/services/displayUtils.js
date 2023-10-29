import c from "../config";
import {useSelector} from "react-redux";
import {useCallback} from "react";

export const formatScore = (score, digits = 2) => {
    if (score === undefined) {
        return c.SCORE_NONE_TEXT;
    } else if (score === "*") {
        return score;
    }
    return score?.toFixed((score - Math.floor(score)) > 0 ? digits : 0);
};
const usePenaltyRoundingMode = () => useSelector((state) => state.contestInfo?.info?.penaltyRoundingMode);
export const useFormatPenalty = () => {
    const mode = usePenaltyRoundingMode();
    return useCallback((penalty) => {
        if (penalty === undefined || penalty === null) {
            return "";
        }
        if (mode === "sum_in_seconds" || mode === "last") {
            return Math.floor(penalty / 60) + ":" + (penalty % 60 < 10 ? "0" : "") + (penalty % 60);
        } else {
            return Math.floor(penalty / 60);
        }
    }, [mode]);
}
export const useNeedPenalty = () => {
    return usePenaltyRoundingMode() !== "zero";
};