import { createApiPost, useWebsocket } from "../utils";
import { BASE_URL_BACKEND, BASE_URL_WS } from "../config";
import { useMemo, useState } from "react";

const apiPath = "/analytics";
const apiPost = createApiPost(BASE_URL_BACKEND + apiPath);

export const useAnalyticsService = () => {
    const [messages, setMessages] = useState({});
    const messagesList = useMemo(() =>
        Object.values(messages)
            .sort((a, b) => b.timeUnixMs - a.timeUnixMs).slice(0, 100),
    [messages]);

    const processEvent = useMemo(() => event => {
        switch (event.type) {
        case "AnalyticsMessageSnapshot":
            setMessages(() => event.messages.reduce((ac, message) => ({ ...ac, [message.id]: message }), {}));
            break;
        case "ModifyAnalyticsMessage":
        case "AddAnalyticsMessage":
            setMessages(messages => ({ ...messages, [event.message.id]: event.message }));
            break;
        }
    }, [setMessages]);
    const handleWSMessage = useMemo(() => e => processEvent(JSON.parse(e.data)),
        [processEvent]);
    useWebsocket(BASE_URL_WS + apiPath, handleWSMessage);

    const createAdvertisement = (messageId, ttlMs) =>
        apiPost("/" + messageId + "/advertisement" + (ttlMs === undefined ? "" : "?ttl=" + (ttlMs * 1000)),
            undefined, "POST");
    const deleteAdvertisement = (messageId) =>
        apiPost("/" + messageId + "/advertisement", undefined, "DELETE");

    const createTickerMessage = (messageId, ttlMs) =>
        apiPost("/" + messageId + "/tickerMessage" + (ttlMs === undefined ? "" : "?ttl=" + (ttlMs * 1000)),
            undefined, "POST");
    const deleteTickerMessage = (messageId) =>
        apiPost("/" + messageId + "/tickerMessage", undefined, "DELETE");

    const makeFeaturedRun = (messageId, mediaType) =>
        apiPost("/" + messageId + "/featuredRun", mediaType, "POST");
    const makeNotFeaturedRun = (messageId) =>
        apiPost("/" + messageId + "/featuredRun", undefined, "DELETE");

    return {
        messagesMap: messages,
        messagesList,
        createAdvertisement,
        deleteAdvertisement,
        createTickerMessage,
        deleteTickerMessage,
        makeFeaturedRun,
        makeNotFeaturedRun,
    };
};
