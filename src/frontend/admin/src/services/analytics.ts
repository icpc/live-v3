import { useCallback, useEffect, useMemo, useState } from "react";
import { useWebsocket } from "@/utils";
import { BASE_URL_BACKEND, BASE_URL_WS } from "@/config";
import { AnalyticsEvent, AnalyticsMessage, ContestInfo, RunInfo, TeamInfo, TeamMediaType } from "@shared/api.ts";
import { createApiGet, createApiPost } from "@shared/utils.ts";

const apiPath = "/analytics";
const apiPost = createApiPost(BASE_URL_BACKEND + apiPath);
const apiGet = createApiGet(BASE_URL_BACKEND + apiPath);

export type FeaturedRunStatus = {
    messageId: string;
    runInfo: RunInfo;
    teamInfo: TeamInfo;
};
export const useAnalyticsService = () => {
    const [messages, setMessages] = useState<{ [id: string]: AnalyticsMessage }>({});
    const messagesList = useMemo(() =>
        Object.values(messages)
            .sort((a: AnalyticsMessage, b: AnalyticsMessage) => b.updateTimeUnixMs - a.updateTimeUnixMs),
    [messages]);

    const processEvent = useCallback((event: AnalyticsEvent) => {
        switch (event.type) {
        case AnalyticsEvent.Type.AnalyticsMessageSnapshot:
            setMessages(() => event.messages.reduce((ac, message) => ({ ...ac, [message.id]: message }), {}));
            break;
        case AnalyticsEvent.Type.UpdateAnalyticsMessage:
            setMessages(messages => ({ ...messages, [event.message.id]: event.message }));
            break;
        }
    }, [setMessages]);
    const handleWSMessage = useCallback((e: { data: string }) => processEvent(JSON.parse(e.data) as AnalyticsEvent),
        [processEvent]);
    useWebsocket<string, void>(BASE_URL_WS + apiPath, handleWSMessage);

    const [contestInfo, setContestInfo] = useState<ContestInfo>(null);
    useEffect(() => {
        apiGet("/contestInfo").then(r => setContestInfo(r as ContestInfo));
    }, []);
    const teams = useMemo(() => {
        const teams = {};
        contestInfo?.teams?.forEach(t => teams[t.id] = t);
        return teams;
    }, [contestInfo]);
    const problems = useMemo(() => {
        const problems = {};
        contestInfo?.problems?.forEach(p => problems[p.id] = p);
        return problems;
    }, [contestInfo]);

    const featuredRunStatus: FeaturedRunStatus | undefined = useMemo(() => {
        const eventWithFeatured = messagesList.find(m => m.featuredRun);
        if (eventWithFeatured) {
            return {
                messageId: eventWithFeatured.id,
                teamInfo: teams[eventWithFeatured.teamId],
                runInfo: eventWithFeatured.runInfo,
            };
        }
        return undefined;
    }, [messagesList, teams]);

    const createAdvertisement = (messageId: string, commentId: string, ttlMs?: number) =>
        apiPost("/" + messageId + `/${commentId}/advertisement` + (ttlMs === undefined ? "" : "?ttl=" + (ttlMs * 1000)),
            undefined, "POST");
    const deleteAdvertisement = (messageId: string, commentId: string) =>
        apiPost("/" + messageId + `/${commentId}/advertisement`, undefined, "DELETE");

    const createTickerMessage = (messageId: string, commentId: string, ttlMs?: number) =>
        apiPost("/" + messageId + `/${commentId}/tickerMessage` + (ttlMs === undefined ? "" : "?ttl=" + (ttlMs * 1000)),
            undefined, "POST");
    const deleteTickerMessage = (messageId: string, commentId: string) =>
        apiPost("/" + messageId + `/${commentId}/tickerMessage`, undefined, "DELETE");

    const makeFeaturedRun = (messageId: string, mediaType: TeamMediaType) =>
        apiPost("/" + messageId + "/featuredRun", mediaType, "POST");
    const makeNotFeaturedRun = (messageId: string) =>
        apiPost("/" + messageId + "/featuredRun", undefined, "DELETE");

    return {
        messagesMap: messages,
        messagesList,
        featuredRunStatus,
        teams,
        problems,
        contestInfo,
        createAdvertisement,
        deleteAdvertisement,
        createTickerMessage,
        deleteTickerMessage,
        makeFeaturedRun,
        makeNotFeaturedRun,
    };
};
