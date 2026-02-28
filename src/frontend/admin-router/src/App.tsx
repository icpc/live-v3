import { AdminLayout } from "./AdminLayout";
import { Box, Typography } from "@mui/material";

const App = () => {
    return (
        <AdminLayout>
            <Box sx={{ p: 3 }}>
                <Typography variant="h4">Admin Router</Typography>
                <Typography variant="body1">Select an admin module from the side menu.</Typography>
            </Box>
        </AdminLayout>
    );
};

export default App;
