import { ContestInfo } from "@shared/api";
import { useCallback, useState } from "react";
import { BASE_URL_WS } from "@/config.ts";
import { useWebsocket } from "../utils";

export const useContestInfo = () => {
    const [contestInfo, setContestInfo] = useState<ContestInfo>();
    const handleWSMessage = useCallback(
        (event) => setContestInfo(JSON.parse(event.data)),
        [],
    );
    useWebsocket(BASE_URL_WS + "/contestInfo", handleWSMessage);
    return contestInfo;
};
