import { DateTime } from "luxon";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { WEBSOCKET_RECONNECT_TIME } from "./config";

export const localStorageGet = <T>(key: string) => JSON.parse(localStorage.getItem(key) ?? "null") as T;
export const localStorageSet = <T>(key: string, value: T) => localStorage.setItem(key, JSON.stringify(value));
export const useLocalStorageState = <T> (key: string, defaultValue: T): [T, (newValue: T) => void] => {
    const [state, setState] = useState<T>(localStorageGet<T>(key) ?? defaultValue);
    const saveState = (v: T) => {
        localStorageSet(key, v);
        setState(v);
    };
    return [state, saveState];
};

export const timeSecondsToDuration = (timeMs?: number) => (timeMs === null || timeMs === undefined) ? "??" : DateTime.fromSeconds(timeMs, { zone: "utc" }).toFormat("H:mm:ss") ;
export const timeMsToDuration = (timeMs?: number) => (timeMs === null || timeMs === undefined) ? "??" : DateTime.fromMillis(timeMs, { zone: "utc" }).toFormat("H:mm:ss");
export const unixTimeMsToLocalTime = (timeMs?: number) => (timeMs === null || timeMs === undefined) ? "??" : DateTime.fromMillis(timeMs, { zone: "local" }).toFormat("HH:mm:ss dd LLL yyyy ZZZZ");

export const useWebsocket = <T, R>(wsUrl: string, handleMessage: (message: MessageEvent<T>) => R) => {
    const [isConnected, setIsConnected] = useState(false);
    const ws = useRef<WebSocket>(null);
    const openSocket = useCallback(() => {
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
    }, [openSocket]);
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

export const useDebounceList = <T> (delay: number): [T[], (newList: T[]) => void, (addElement: T) => void] => {
    const [addCache, setAddCache] = useState<T[]>([]);
    const [debouncedValue, setDebouncedValue] = useState<T[]>([]);
    const add = (value: T) => setAddCache(cache => [value, ...cache]);
    const pushCache = () => {
        const currentCache = addCache;
        setAddCache([]);
        setDebouncedValue(value => [...currentCache, ...value]);
    };
    useEffect(() => {
        const handler = setTimeout(pushCache, delay);
        return () => clearTimeout(handler);
    }, [addCache]);
    return [debouncedValue, setDebouncedValue, add];
};

export const useFillHeight = (ref?: HTMLElement) => {
    // function getWindowDimensions() {
    //     const { innerWidth: width, innerHeight: height } = window;
    //     return {
    //         width,
    //         height
    //     };
    // }
    //
    // export default function useWindowDimensions() {
    const [windowHeight, setWindowHeight] = useState(window.innerHeight);

    useEffect(() => {
        function handleResize() {
            setWindowHeight(window.innerHeight);
        }

        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    return useMemo(() => windowHeight - (ref?.offsetTop ?? 0),
        [windowHeight, ref?.offsetTop]);
};

export const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})([a-f\d]{2}?)$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
};

/*
Returns true if dark (black) color better for given backgroundColor and false otherwise.
 */
export const isShouldUseDarkColor = (backgroundColor: string) => {
    const rgb = hexToRgb(backgroundColor);
    if (!rgb) {
        return false;
    }
    const { r, g, b } = rgb;
    // http://www.w3.org/TR/AERT#color-contrast
    const brightness = Math.round(((r * 299) +
        (g * 587) +
        (b * 114)) / 1000);

    return brightness > 125;
};

export const setFavicon = (svg: string) => {
    const link = document.createElement("link");
    link.type = "image/x-icon";
    link.rel = "shortcut icon";
    link.href = "data:image/svg+xml;base64," + btoa(svg);
    document.head.appendChild(link);
};
