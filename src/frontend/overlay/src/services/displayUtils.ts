import c from "../config";
import { useCallback } from "react";
import { PenaltyRoundingMode } from "@shared/api";
import { useAppSelector } from "@/redux/hooks";

export const formatScore = (score: "*" | number | undefined, digits = 2) => {
    if (score === undefined) {
        return c.SCORE_NONE_TEXT;
    } else if (score === "*") {
        return score;
    }
    return score?.toFixed((score - Math.floor(score)) > 0 ? digits : 0);
};
const usePenaltyRoundingMode = (): PenaltyRoundingMode => useAppSelector((state) => state.contestInfo?.info?.penaltyRoundingMode);
export const useFormatPenalty: () => (penalty: (number | undefined | null)) => string = () => {
    const mode = usePenaltyRoundingMode();
    return useCallback((penalty: number | undefined | null): string => {
        if (penalty === undefined || penalty === null) {
            return "";
        }
        if (mode === "sum_in_seconds" || mode === "last") {
            return Math.floor(penalty / 60) + ":" + (penalty % 60 < 10 ? "0" : "") + (penalty % 60);
        } else {
            return Math.floor(penalty / 60) + "";
        }
    }, [mode]);
};
export const useNeedPenalty = (): boolean => {
    return usePenaltyRoundingMode() !== "zero";
};
