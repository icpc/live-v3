import { Typography, List, ListItem, ListItemButton, ListItemText, Paper, Container } from "@mui/material";

export function LoginAdmin() {
    return (
        <Container maxWidth="sm" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>Session Management</Typography>
                <Typography variant="body1" paragraph>
                    Manage your current session.
                </Typography>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/login">
                            <ListItemText primary="Login" secondary="Sign in to the application" />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href="/logout">
                            <ListItemText primary="Logout" secondary="Sign out from the application" />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Paper>
        </Container>
    );
}
