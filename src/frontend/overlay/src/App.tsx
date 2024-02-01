import _, { now } from "lodash";
import React, { useEffect, useRef } from "react";
import MainLayout from "./components/layouts/MainLayout";
import { StatusLayout } from "./components/layouts/StatusLayout";
import c from "./config";
import { WEBSOCKETS } from "./consts";
import { pushLog } from "./redux/debug";
import { setWebsocketStatus, WebsocketStatus } from "./redux/status";
import { WEBSOCKET_HANDLERS } from "./services/ws/ws";
import { useMountEffect } from "./utils/hooks/useMountEffect";
import { useAppDispatch } from "@/redux/hooks";

const safeHandleMessage = <H extends (...args: unknown[]) => unknown,>(ws: WebSocket, wsName: string, handler: H): H => {
    let messagesMadeLastSecond = 0;
    const getEpochSecond = () => Math.floor((new Date()).getTime() / 1000);
    messagesMadeLastSecond++;
    let lastSecond = getEpochSecond();
    return ((...args) => {
        const nowSecond = getEpochSecond();
        if(nowSecond != lastSecond) {
            messagesMadeLastSecond = 0;
            lastSecond = nowSecond;
        }
        messagesMadeLastSecond++;
        if(messagesMadeLastSecond > 50) {
            console.error(`Backend is overloading us with data on ${wsName}. Closing socket.`);
            ws.close();
            return;
        }
        return handler(...args);
    }) as H;
};

const useMakeWebsocket = (dispatch) => (ws, wsName, handleMessage) => {
    const openSocket = () => {
        dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTING));
        ws.current = new WebSocket(`${c.BASE_URL_WS}/${WEBSOCKETS[wsName]}`);
        ws.current.onopen = () => {
            dispatch(pushLog(`Connected to WS ${wsName}`));
            dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTED));
        };
        ws.current.onclose = () => {
            dispatch(pushLog(`Disconnected from WS ${wsName}`));
            dispatch(setWebsocketStatus(wsName, WebsocketStatus.DISCONNECTED));
            ws.current = null;
            setTimeout(openSocket, c.WEBSOCKET_RECONNECT_TIME);
        };
        ws.current.onmessage = safeHandleMessage(ws.current, wsName, handleMessage);
    };
    openSocket();
    return () => ws.current.close();
};


function App() {
    const dispatch = useAppDispatch();
    const makeWebsocket = useMakeWebsocket(dispatch);
    const wss = Object.fromEntries(Object.keys(WEBSOCKETS).map((key) => {
        // since WEBSOCKETS is static.
        // eslint-disable-next-line react-hooks/rules-of-hooks
        return [key, useRef(null)];
    }));

    useMountEffect(() => {
        _.forEach(wss, (v, k) => {
            makeWebsocket(v, k, WEBSOCKET_HANDLERS[k](dispatch));
        });
    });

    useEffect(() => {
        document.addEventListener("error", (message) => {
            dispatch(pushLog(`Global error on document: ${message}`));
        });
    }, [dispatch]);

    const noStatus = window.location.search.includes("noStatus");

    return (
        <>
            <MainLayout/>
            {noStatus ? null : <StatusLayout/>}
        </>
    );
}

export default App;

