import React, { useCallback } from "react";
import Ansi from "ansi-to-react";
import { BASE_URL_WS } from "../config";
import { Box, Grid } from "@mui/material";
import { useDebounceList, useWebsocket } from "../utils";

function BackendLog() {
    const [messages, , addMessages] = useDebounceList(100);
    const handleWSMessage = useCallback(event => addMessages(event.data),
        []);
    useWebsocket(BASE_URL_WS + "/backendLog", handleWSMessage);

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
