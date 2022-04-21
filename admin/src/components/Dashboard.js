import React from "react";

import "../App.css";
import Container from "@mui/material/Container";
import TickerMessage from "./TickerMessage";
import Controls from "./Controls";
import Advertisement from "./Advertisement";
import Picture from "./Picture";
import ScoreboardSettings from "./ScoreboardSettings";
import AdvancedProperties from "./AdvancedProperties";


import Paper from "@mui/material/Paper";
const elements = {
    "Controls": <Controls/>,
    "Advertisement": <Advertisement/>,
    "Picture": <Picture/>,
    "Scoreboard": <ScoreboardSettings/>,
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
            gridTemplateColumns: { "md": "repeat(2, 6fr)", "sm": "repeat(1, 6fr)" },
            gap: 0.25 }}>
            {Object.entries(elements).map(([name, element]) => (
                <Paper elevation={3} key={name} sx={{ margin: 2, padding: 2 }}>
                    {element}
                </Paper>
            ))}
        </Container>
    );
}

export default Dashboard;
