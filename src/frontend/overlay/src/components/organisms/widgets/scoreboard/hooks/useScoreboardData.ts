import { useMemo } from "react";
import { useAppSelector } from "@/redux/hooks";
import { ContestInfo, OptimismLevel, ProblemInfo, TeamInfo } from "@shared/api";
import { ScoreboardData } from "@/redux/contest/scoreboard";

export function useScoreboardRows(
    optimismLevel: OptimismLevel,
    selectedGroup: string,
) {
    const order = useAppSelector(
        state => state.scoreboard[optimismLevel]?.orderById,
    );
    const teamsId = useAppSelector(state => state.contestInfo.info?.teamsId);

    return useMemo(() => {
        if (teamsId === undefined || order === undefined) {
            return [];
        }

        const result = Object.entries(order).filter(
            ([k]) => 
                selectedGroup === "all" ||
                (teamsId[k]?.groups ?? []).includes(selectedGroup),
        );

        if (selectedGroup !== "all") {
            const rowsNumbers = result.map(([_, b]) => b);
            rowsNumbers.sort((a, b) => (a > b ? 1 : a == b ? 0 : -1));

            const mapping = new Map();
            for (let i = 0; i < rowsNumbers.length; i++) {
                mapping.set(rowsNumbers[i], i);
            }

            for (let i = 0; i < result.length; i++) {
                result[i][1] = mapping.get(result[i][1]);
            }
        }

        return result;
    }, [order, teamsId, selectedGroup]);
};


export function useScoreboardData(optimismLevel: OptimismLevel): {
    scoreboardData: ScoreboardData,
    normalScoreboardData: ScoreboardData,
    contestData: ContestInfo & {
        teamsId: Record<TeamInfo["id"], TeamInfo>;
        problemsId: Record<ProblemInfo["id"], ProblemInfo>;
    },
} {
    const scoreboardData = useAppSelector(
        state => state.scoreboard[optimismLevel],
    );
    const normalScoreboardData = useAppSelector(
        state => state.scoreboard[OptimismLevel.normal],
    );
    const contestData = useAppSelector(state => state.contestInfo.info);

    return {
        scoreboardData,
        normalScoreboardData,
        contestData,
    };
};
