import React, { useEffect, useState, useRef } from "react";
import Ansi from "ansi-to-react";

import "../App.css";
import { BASE_URL_WS } from "../config";

const apiUrl = () => {
    return BASE_URL_WS + "/backendLog";
};

function BackendLog() {
    const [messages, setMessages] = useState([]);
    const [isConnectionOpen, setConnectionOpen] = useState(false);

    const ws = useRef();

    useEffect(() => {
        ws.current = new WebSocket(apiUrl());

        ws.current.onopen = () => {
            console.log("Connection opened");
            setConnectionOpen(true);
        };

        ws.current.onmessage = (event) => {
            const data = event.data;
            setMessages((_messages) => [data, ..._messages]);
        };

        return () => {
            console.log("Cleaning up...");
            ws.current.close();
        };
    }, []);

    const scrollTarget = useRef(null);

    React.useEffect(() => {
        if (scrollTarget.current) {
            scrollTarget.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages.length]);

    return (<div>
        { messages.map((message, index) =>
            <p key = { index } >
                <Ansi>
                    { message }
                </Ansi>
            </p>
        )}
    </div>
    );
}

export default BackendLog;
