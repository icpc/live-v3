import { Typography, List, ListItem, ListItemButton, ListItemText, Paper, Container } from "@mui/material";

export function PCMSAdmin() {
    return (
        <Container maxWidth="md" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>PCMS Exporter</Typography>
                <Typography variant="body1" paragraph>
                    Standard PCMS standings in XML and HTML formats.
                </Typography>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/pcms/standings.html">
                            <ListItemText primary="PCMS scoreboard (HTML)" secondary="View current scoreboard" />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/pcms/standings.xml">
                            <ListItemText primary="PCMS standings.xml" secondary="Download XML standings" />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Paper>
        </Container>
    );
}
