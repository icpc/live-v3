import { useEffect, useState, useRef, startTransition } from "react";
import c from "@/config";


export type AnimatingTeam = {
    fromPos: number;
    toPos: number;
    startTime: number;
};

export function useAnimatingTeams(rows: [string, number][]) {
    const [animatingTeams, setAnimatingTeams] = useState<
        Map<string, AnimatingTeam>
    >(new Map());

    const prevOrderRef = useRef<Map<string, number>>(new Map());

    useEffect(() => {
        const prevOrder = prevOrderRef.current;
        const newAnimating = new Map<string, AnimatingTeam>();

        for (const [teamId, newPos] of rows) {
            const oldPos = prevOrder.get(teamId);
            if (oldPos !== undefined && oldPos !== newPos) {
                newAnimating.set(teamId, {
                    fromPos: oldPos,
                    toPos: newPos,
                    startTime: performance.now(),
                });
            }
        }

        if (newAnimating.size > 0) {
            startTransition(() => {
                setAnimatingTeams((prev) => {
                    const merged = new Map(prev);
                    for (const [k, v] of newAnimating) {
                        merged.set(k, v);
                    }
                    return merged;
                });
            });
        }

        prevOrderRef.current = new Map(rows);
    }, [rows]);

    useEffect(() => {
        if (animatingTeams.size === 0) return;

        const timeout = setTimeout(() => {
            const now = performance.now();
            startTransition(() => {
                setAnimatingTeams((prev) => {
                    const filtered = new Map<string, AnimatingTeam>();
                    for (const [k, v] of prev) {
                        if (
                            now - v.startTime <
                            c.SCOREBOARD_ROW_TRANSITION_TIME
                        ) {
                            filtered.set(k, v);
                        }
                    }
                    return filtered;
                });
            });
        }, c.SCOREBOARD_ROW_TRANSITION_TIME);

        return () => clearTimeout(timeout);
    }, [animatingTeams]);

    return animatingTeams;
};
