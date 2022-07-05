import _ from "lodash";
import React, { useEffect, useRef } from "react";
import { useDispatch } from "react-redux";
import MainLayout from "./components/layouts/MainLayout";
import { StatusLayout } from "./components/layouts/StatusLayout";
import { BASE_URL_WS, WEBSOCKET_RECONNECT_TIME } from "./config";
import { WEBSOCKETS } from "./consts";
import { pushLog } from "./redux/debug";
import { setWebsocketStatus, WebsocketStatus } from "./redux/status";
import { WEBSOCKET_HANDLERS } from "./services/ws/ws";

const useMakeWebsocket = (dispatch) => (ws, wsName, handleMessage) => {
    const openSocket = () => {
        dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTING));
        ws.current = new WebSocket(`${BASE_URL_WS}/${WEBSOCKETS[wsName]}`);
        ws.current.onopen = () => {
            dispatch(pushLog(`Connected to WS ${wsName}`));
            dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTED));
        };
        ws.current.onclose = () => {
            dispatch(pushLog(`Disconnected from WS ${wsName}`));
            dispatch(setWebsocketStatus(wsName, WebsocketStatus.DISCONNECTED));
            ws.current = null;
            setTimeout(openSocket, WEBSOCKET_RECONNECT_TIME);
        };
        ws.current.onmessage = handleMessage;
    };
    openSocket();
    return () => ws.current.close();
};


function App() {
    const dispatch = useDispatch();
    const makeWebsocket = useMakeWebsocket(dispatch);
    const wss = Object.fromEntries(Object.keys(WEBSOCKETS).map((key) => {
        return [key, useRef(null)];
    }));

    useEffect(() => {
        _.forEach(wss, (v, k) => {
            makeWebsocket(v, k, WEBSOCKET_HANDLERS[k](dispatch));
        });
    }, []);

    useEffect(() => {
        document.addEventListener("error", (message) => {
            dispatch(pushLog(`Global error on document: ${message}`));
        });
    }, []);

    const noStatus = window.location.search.includes("noStatus");

    return (
        <>
            <MainLayout/>
            {noStatus ? null : <StatusLayout/>}
        </>
    );
}

export default App;

