import { createApiPost } from "shared-code/utils";
import { useWebsocket } from "../utils";
import { BASE_URL_BACKEND, BASE_URL_WS } from "../config";
import { useMemo, useState, useCallback } from "react";
import { TeamId } from "@shared/api";

export interface Team {
    id: TeamId;
    name: string;
    score: number;
}

export interface ScoreEntry {
    teamId: TeamId;
    score: number;
}

interface TeamSpotlightService {
    teamsList: Team[];
    addScore: (entries: ScoreEntry[]) => unknown;
    isLoading: boolean;
    error: string | null;
}

const API_PATH = "/teamSpotlight";
const API_ENDPOINTS = {
    ADD_SCORE: "/addScore",
} as const;

const apiPost = createApiPost(BASE_URL_BACKEND + API_PATH);

async function addScoreApi(requests: ScoreEntry[]) {
    try {
        await apiPost(API_ENDPOINTS.ADD_SCORE, requests, "POST");
    } catch (error) {
        console.error(`Failed to add scores: ${error}`);
        throw new Error(`Failed to add scores`);
    }
}

export const useTeamSpotlightService = (): TeamSpotlightService => {
    const [teams, setTeams] = useState<Team[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const teamsList = useMemo(() =>
        [...teams].sort((a, b) => b.score - a.score),
        [teams]
    );

    const handleWSMessage = useCallback((event: MessageEvent) => {
        try {
            const data = JSON.parse(event.data) as Team[];
            setTeams(data);
            setError(null);
        } catch (error) {
            console.error(`Failed to parse WebSocket message: ${error}`);
            setError("Failed to update team data");
        }
    }, []);

    useWebsocket(BASE_URL_WS + API_PATH, handleWSMessage);

    const addScore = useCallback(async (entires: ScoreEntry[]) => {
        if (entires.length) {
            throw new Error("no score entries provided");
        }

        setIsLoading(true);
        setError(null);

        try {
            await addScoreApi(entires);
        } catch (error) {
            const errorMessage = error instanceof Error
                ? error.message
                : "An unexpected error occurred";
            setError(errorMessage);
            throw error;
        } finally {
            setIsLoading(false);
        }
    }, []);

    return {
        teamsList,
        addScore,
        isLoading,
        error,
    };
};

