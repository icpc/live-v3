import React from "react";

import "../App.css";
import { Paper, Container, Typography } from "@mui/material";
import { grey } from "@mui/material/colors";
import TickerMessage from "./TickerMessage";
import Controls from "./Controls";
import Advertisement from "./Advertisement";
import Title from "./Title";
import Picture from "./Picture";
import ScoreboardManager from "./ScoreboardManager";

const elements = {
    "Controls": <Controls/>,
    "Advertisement": <Advertisement/>,
    "Title": <Title/>,
    "Picture": <Picture/>,
    "Scoreboard": <ScoreboardManager/>,
    "Ticker": <TickerMessage/>,
    // "TeamView": <TeamView/>,
    // "Advanced Properties": <AdvancedProperties/>,
};

function Dashboard() {
    return (
        <Container maxWidth="xl" sx={{
            pt: 6,
            display: "grid",
            width: "100%",
            gridTemplateColumns: { "md": "repeat(2, 4fr)", "sm": "repeat(1, 4fr)" },
            gap: 0.25 }}>
            {Object.entries(elements).map(([name, element]) => (
                <Paper
                    elevation={1}
                    key={name}
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        margin: 1,
                        padding: 1 }}
                >
                    <Typography color={grey[400]} variant="h6">
                        {name}
                    </Typography>
                    {element}
                </Paper>
            ))}
        </Container>
    );
}

export default Dashboard;
