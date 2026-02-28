import React from "react";
import { Box, Typography } from "@mui/material";
import { AdminLayout } from "admin-router";

function App() {
    return (
        <AdminLayout>
            <Box sx={{ p: 3 }}>
                <Typography variant="h4">Contest Info</Typography>
                <Typography variant="body1">This is a placeholder for the Contest Info page.</Typography>
            </Box>
        </AdminLayout>
    );
}

export default App;
