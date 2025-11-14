import React from "react";

import "../App.css";
import { Paper, Container, Typography, ContainerProps } from "@mui/material";
import { grey } from "@mui/material/colors";

type Layout = "oneColumn" | "twoColumns";

interface DashboardProps {
    elements: Record<string, React.ReactNode>;
    layout?: Layout;
    maxWidth?: ContainerProps["maxWidth"];
}

function Dashboard({
    elements,
    layout,
    maxWidth,
}: DashboardProps): React.ReactElement {
    const gridLayouts: Record<Layout, { md: string; sm: string }> = {
        oneColumn: { md: "repeat(1, 4fr)", sm: "repeat(1, 4fr)" },
        twoColumns: { md: "repeat(2, 4fr)", sm: "repeat(1, 4fr)" },
    };

    const selected = gridLayouts[layout] ?? gridLayouts.twoColumns;

    return (
        <Container
            maxWidth={maxWidth}
            sx={{
                pt: 6,
                display: "grid",
                gridTemplateColumns: selected,
                gap: 0.25,
            }}
        >
            {Object.entries(elements).map(([name, element]) => (
                <Paper
                    elevation={1}
                    key={name}
                    sx={{
                        display: "flex",
                        width: "96%",
                        flexDirection: "column",
                        alignItems: "center",
                        m: 1,
                        p: 1,
                    }}
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
