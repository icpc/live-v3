import { DateTime } from "luxon";
import { useEffect, useMemo, useRef, useState } from "react";
import { WEBSOCKET_RECONNECT_TIME } from "./config";

export const localStorageGet = key => JSON.parse(localStorage.getItem(key) ?? "null");
export const localStorageSet = (key, value) => localStorage.setItem(key, JSON.stringify(value));
export const useLocalStorageState = (key, defaultValue) => {
    const [state, setState] = useState(localStorageGet(key) ?? defaultValue);
    const saveState = v => {
        localStorageSet(key, v);
        setState(v);
    };
    return [state, saveState];
};

export const createApiPost = (apiUrl) =>
    function (path, body = {}, method = "POST") {
        const requestOptions = {
            method: method,
            headers: { "Content-Type": "application/json" },
            body:  method === "GET" ? undefined : JSON.stringify(body),
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => response.json())
            .then(response => {
                if (response.status !== "ok") {
                    throw new Error("Server return not ok status: " + response);
                }
                return response;
            });
    };
export const createApiGet = (apiUrl) =>
    function (path, body = undefined) {
        const requestOptions = {
            headers: { "Content-Type": "application/json" },
            body:  body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => response.json());
    };

export const timeMsToDuration = (timeMs) => DateTime.fromMillis(timeMs, { zone: "utc" }).toFormat("H:mm:ss");
export const unixTimeMsToLocalTime = (timeMs) => DateTime.fromMillis(timeMs, { zone: "local" }).toFormat("HH:mm:ss");

export const useWebsocket = (wsUrl, handleMessage) => {
    const [isConnected, setIsConnected] = useState(false);
    const ws = useRef(null);
    const openSocket = useMemo(() => () => {
        ws.current = new WebSocket(wsUrl);
        ws.current.onopen = () => {
            setIsConnected(true);
            console.info(`Connected to WS ${wsUrl}`);
        };
        ws.current.onclose = () => {
            setIsConnected(false);
            console.warn(`Disconnected from WS ${wsUrl}`);
            setTimeout(openSocket, WEBSOCKET_RECONNECT_TIME);
        };
        ws.current.onmessage = handleMessage;
    }, [wsUrl, handleMessage]);
    useEffect(() => {
        openSocket();
        return () => ws.current?.close();
    }, [wsUrl, handleMessage]);
    return isConnected;
};

export const useDebounce = (value, delay) => {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const handler = setTimeout(() => setDebouncedValue(value), delay);
        return () => clearTimeout(handler);
    }, [value, delay]);
    return debouncedValue;
};

export const useDebounceList = (delay) => {
    const [addCache, setAddCache] = useState([]);
    const [debouncedValue, setDebouncedValue] = useState([]);
    const add = (value) => setAddCache(cache => [ value, ...cache ]);
    const pushCache = () => {
        const currentCache = addCache;
        setAddCache([]);
        setDebouncedValue(value => [ ...currentCache, ...value ]);
    };
    useEffect(() => {
        const handler = setTimeout(pushCache, delay);
        return () => clearTimeout(handler);
    }, [addCache]);
    return [debouncedValue, setDebouncedValue, add];
};
