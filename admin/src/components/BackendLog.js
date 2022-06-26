import React, { useEffect, useState, useRef } from "react";

import "../App.css";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
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
            setMessages((_messages) => [..._messages, data]);
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
        <Table align={"center"}>
            <TableBody>
                { messages.slice(-20).reverse().map((message, index) =>
                    <TableRow key={ index }>
                        <TableCell>
                            { message }
                        </TableCell>
                    </TableRow>
                )}
            </TableBody>
        </Table>
    </div>
    );
}

export default BackendLog;
