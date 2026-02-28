import {
    Typography,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Paper,
    Container,
} from "@mui/material";

export function IcpcAdmin() {
    return (
        <Container maxWidth="md" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>
                    ICPC Global CSV Exporter
                </Typography>
                <Typography variant="body1" paragraph>
                    Manage and access ICPC-compatible data feeds.
                </Typography>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton
                            component="a"
                            href="/icpc/standings.csv"
                        >
                            <ListItemText
                                primary="ICPC standings.csv"
                                secondary="Download CSV for icpc.global"
                            />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Paper>
        </Container>
    );
}
