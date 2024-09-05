import { ContestInfo } from "@shared/api"
import {useCallback, useState} from "react";
import { BACKEND_PORT, WS_PROTO } from "@/config.ts";
import { useWebsocket } from "../utils";

export const useContestInfo = () => {
    const [contestInfo, setContestInfo] = useState<ContestInfo>();
    const handleWSMessage = useCallback(event => setContestInfo(JSON.parse(event.data)),
        []);
    useWebsocket(import.meta.env.VITE_WEBSOCKET_URL ?? (WS_PROTO + window.location.hostname + ":" + BACKEND_PORT + "/api/overlay/contestInfo"), handleWSMessage);
    return contestInfo;
}
