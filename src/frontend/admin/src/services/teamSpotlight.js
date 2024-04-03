import { createApiPost } from "shared-code/utils";
import { useWebsocket } from "../utils";
import { BASE_URL_BACKEND, BASE_URL_WS } from "../config";
import { useMemo, useState } from "react";

const apiPath = "/teamSpotlight";
const apiPost = createApiPost(BASE_URL_BACKEND + apiPath);

const addScore = (requests) =>
    apiPost("/addScore", requests, "POST");

export const useTeamSpotlightService = () => {
    const [teams, setTeams] = useState([]);
    const teamsList = useMemo(() =>
        teams.sort((a, b) => b.score - a.score),
    [teams]);

    const handleWSMessage = useMemo(() => e => setTeams(JSON.parse(e.data)),
        [setTeams]);
    useWebsocket(BASE_URL_WS + apiPath, handleWSMessage);

    return {
        teamsList,
        addScore,
    };
};
