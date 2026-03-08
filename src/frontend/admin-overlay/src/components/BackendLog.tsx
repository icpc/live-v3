import React, { useCallback } from "react";
import { AnsiUp } from "ansi_up";
import { BASE_URL_WS } from "../config";
import { Box, Grid } from "@mui/material";
import { useDebounceList, useWebsocket } from "../utils";

const ansiUp = new AnsiUp();

function BackendLog(): React.ReactElement {
    const [messages, , addMessages] = useDebounceList<string>(100);

    const handleWSMessage = useCallback(
        (event: MessageEvent<string>) => {
            addMessages(event.data);
        },
        [addMessages],
    );

    useWebsocket(BASE_URL_WS + "/backendLog", handleWSMessage);

    return (
        <Grid
            sx={{
                display: "flex",
                alignContent: "center",
                justifyContent: "center",
                alignItems: "center",
                flexDirection: "column",
            }}
        >
            <Box sx={{ width: { md: "75%", sm: "100%", xs: "100%" } }}>
                {messages.map((message, index) => {
                    const html = ansiUp.ansi_to_html(message);
                    return (
                        <p
                            key={index}
                            style={{
                                whiteSpace: "pre-wrap",
                                fontFamily: "monospace",
                            }}
                            dangerouslySetInnerHTML={{ __html: html }}
                        />
                    );
                })}
            </Box>
        </Grid>
    );
}

export default BackendLog;
