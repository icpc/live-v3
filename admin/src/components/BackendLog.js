import React, { useEffect, useState, useRef } from "react";
import Ansi from "ansi-to-react";

import "../App.css";
import { BASE_URL_WS } from "../config";
import { Box, Grid } from "@mui/material";

const apiUrl = () => {
    return BASE_URL_WS + "/backendLog";
};

function BackendLog() {
    const [messages, setMessages] = useState([]);
    const ws = useRef(null);

    useEffect(() => {
        ws.current = new WebSocket(apiUrl());

        ws.current.onmessage = (event) => {
            setMessages((_messages) => [event.data, ..._messages]);
        };

        return () => {
            ws.current?.close();
        };
    }, []);

    const scrollTarget = useRef(null);

    React.useEffect(() => {
        if (scrollTarget.current) {
            scrollTarget.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages.length]);

    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column" }}>
        <Box sx={{ width: { "md": "75%", "sm": "100%", "xs": "100%" } }}>
            { messages.map((message, index) =>
                <p key = { index } >
                    <Ansi>
                        { message }
                    </Ansi>
                </p>
            )}
        </Box>
    </Grid>
    );
}

export default BackendLog;
