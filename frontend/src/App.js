import React, { useCallback, useEffect, useRef } from "react";
import { useDispatch } from "react-redux";
import MainLayout from "./components/layouts/MainLayout";
import { StatusLayout } from "./components/layouts/StatusLayout";
import { pushLog } from "./redux/debug";
import { setWebsocketStatus, WebsocketStatus } from "./redux/status";
import { handleMessage } from "./services/ws/mainScreen";

function App() {
    const dispatch = useDispatch();
    const ws = useRef(null);

    useEffect(() => {
        dispatch(setWebsocketStatus(WebsocketStatus.CONNECTING));
        ws.current = new WebSocket("ws://localhost:8080/overlay/mainScreen");
        ws.current.onopen = () => {
            dispatch(pushLog("Connected to WS"));
            return dispatch(setWebsocketStatus(WebsocketStatus.CONNECTED));
        };
        ws.current.onclose = () => {
            dispatch(pushLog("Disconnected to WS"));
            return dispatch(setWebsocketStatus(WebsocketStatus.DISCONNECTED));
        };
        gettingData();
        return () => ws.current.close();
    }, [ws]);

    const gettingData = useCallback(() => {
        if (!ws.current) return;
        ws.current.onmessage = handleMessage(dispatch);
    }, [ws]);

    return (
        <>
            <MainLayout/>
            <StatusLayout/>
        </>
    );
}

export default App;
