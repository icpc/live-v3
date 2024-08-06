import { createContext, useCallback, useContext, useEffect, useMemo, useRef } from "react";
import { ADMIN_ACTIONS_WS_URL, WEBSOCKET_RECONNECT_TIME } from "@/config.ts";

export type ReloadHandler = (url: string) => void;

export interface ReloadHandleService {
    subscribe: (handler: ReloadHandler) => void;
    unsubscribe: (handler: ReloadHandler) => void;
}

export class ReloadHandleServiceImpl {
    handlers = new Set<ReloadHandler>;

    subscribe(handler: ReloadHandler) {
        this.handlers.add(handler);
    }

    unsubscribe(handler: ReloadHandler) {
        this.handlers.delete(handler);
    }

    handle(url: string) {
        this.handlers.forEach(h => h(url));
    }
}

export const useReloadHandleService: () => ReloadHandleService = () => {
    const reloadHandleService = useMemo(() => {
        console.debug("Create new ReloadHandleServiceImpl");
        return new ReloadHandleServiceImpl();
    }, []);

    const wsRef = useRef<WebSocket>(null);
    const closedRef = useRef(false);
    const openWS = useCallback(() => {
        if (closedRef.current) {
            return;
        }
        const ws = new WebSocket(ADMIN_ACTIONS_WS_URL);
        ws.onmessage = ({ data }) => reloadHandleService.handle(data as string);
        ws.onclose = () => {
            wsRef.current = null;
            setTimeout(() => {
                console.debug("Reconnecting WebSocket for admin actions");
                openWS();
            }, WEBSOCKET_RECONNECT_TIME);
        };
        wsRef.current = ws;
    }, [reloadHandleService]);

    useEffect(() => {
        console.info("Connecting WebSocket for admin actions");
        openWS();

        return () => {
            console.info("Destroyed WebSocket for admin actions");
            closedRef.current = true;
            if (wsRef.current?.readyState === 1) {
                wsRef.current?.close();
            }
        };
    }, [openWS]);

    return reloadHandleService;
};

export const ReloadHandleContext = createContext<ReloadHandleService>(null);

export const useReloadHandler = () => {
    return useContext(ReloadHandleContext);
};
