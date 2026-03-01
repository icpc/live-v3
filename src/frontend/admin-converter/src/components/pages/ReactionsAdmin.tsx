import { Typography, List, ListItem, ListItemButton, ListItemText, Paper, Container } from "@mui/material";

export function ReactionsAdmin() {
    return (
        <Container maxWidth="md" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>Reactions Exporter</Typography>
                <Typography variant="body1" paragraph>
                    Manage and access reaction videos data.
                </Typography>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/reactions/contestInfo.json">
                            <ListItemText primary="contestInfo.json" secondary="Contest metadata for reactions" />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/reactions/runs.json">
                            <ListItemText primary="runs.json" secondary="List of short run info" />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/reactions/fullRuns.json">
                            <ListItemText primary="fullRuns.json" secondary="List of full run info including videos" />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/reactions/fullRuns/id">
                            <ListItemText primary="Full run for run id" secondary="Fetch detailed info for a specific run" />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Paper>
        </Container>
    );
}
