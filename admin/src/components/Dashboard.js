import React from "react";

import "../App.css";
import { Paper, Container, Typography } from "@mui/material";
import { grey } from "@mui/material/colors";
import PropTypes from "prop-types";

function Dashboard(props) {
    const defaultLayout = "twoColumns";
    const defaultMaxWidth = "xl";

    const gridLayouts = {
        "oneColumn": { "md": "repeat(1, 4fr)", "sm": "repeat(1, 4fr)" },
        "twoColumns": { "md": "repeat(2, 4fr)", "sm": "repeat(1, 4fr)" }
    };

    return (
        <Container maxWidth={props.maxWidth !== undefined ? props.maxWidth : defaultMaxWidth}
            sx={{
                pt: 6,
                display: "grid",
                gridTemplateColumns: (gridLayouts[props.layout] !== undefined ? gridLayouts[props.layout] : gridLayouts[defaultLayout]),
                gap: 0.25 }}>
            {Object.entries(props.elements).map(([name, element]) => (
                <Paper
                    elevation={1}
                    key={name}
                    maxWidth="md"
                    sx={{
                        display: "flex",
                        width: "96%",
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

Dashboard.propTypes = {
    maxWidth: PropTypes.string,
    layout: PropTypes.oneOf(["oneColumn", "twoColumns"]),
    elements: PropTypes.objectOf(PropTypes.element),
};

export default Dashboard;
