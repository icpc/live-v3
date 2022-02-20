import _ from "lodash";
import React, { useEffect, useRef } from "react";
import { useDispatch } from "react-redux";
import MainLayout from "./components/layouts/MainLayout";
import { StatusLayout } from "./components/layouts/StatusLayout";
import { WEBSOCKETS } from "./consts";
import { pushLog } from "./redux/debug";
import { setWebsocketStatus, WebsocketStatus } from "./redux/status";
import { WEBSOCKET_HANDLERS } from "./services/ws/ws";

const useMakeWebsocket = (dispatch) => (ws, wsName, handleMessage) => {
    dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTING));
    ws.current = new WebSocket(`ws://localhost:8080/overlay/${wsName}`);
    ws.current.onopen = () => {
        dispatch(pushLog(`Connected to WS ${wsName}`));
        dispatch(setWebsocketStatus(wsName, WebsocketStatus.CONNECTED));
    };
    ws.current.onclose = () => {
        dispatch(pushLog(`Disconnected from WS ${wsName}`));
        dispatch(setWebsocketStatus(wsName, WebsocketStatus.DISCONNECTED));
    };
    ws.current.onmessage = handleMessage;
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

    return (
        <>
            <MainLayout/>
            <StatusLayout/>
        </>
    );
}

export default App;
