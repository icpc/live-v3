import React from "react";
import Ansi from "ansi-to-react";
import { BASE_URL_WS } from "../config";
import { Box, Grid } from "@mui/material";
import { useDebounceList, useWebsocket } from "../utils";

const apiUrl = () => {
    return BASE_URL_WS + "/backendLog";
};

function BackendLog() {
    const [messages, , addMessages] = useDebounceList(100);
    useWebsocket(apiUrl(), event => {
        addMessages(event.data);
    });

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
