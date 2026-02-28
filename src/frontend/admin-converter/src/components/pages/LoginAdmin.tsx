import { Typography, Button, Paper, Container, Stack } from "@mui/material";
import { getLoginUrl, getLogoutUrl, useAdminSession } from "../../session";

export function LoginAdmin() {
    const sessionInfo = useAdminSession();
    const isLoggedIn = sessionInfo?.loggedIn === true;

    return (
        <Container maxWidth="sm" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>
                    {isLoggedIn ? "Logout" : "Login"}
                </Typography>
                <Typography variant="body1" paragraph>
                    {sessionInfo === null
                        ? "Loading session state..."
                        : isLoggedIn
                          ? `Logged in as ${sessionInfo.username ?? "unknown user"}.`
                          : "You are not logged in."}
                </Typography>
                <Stack direction="row" spacing={2}>
                    {isLoggedIn ? (
                        <Button
                            variant="contained"
                            color="primary"
                            component="a"
                            href={getLogoutUrl()}
                        >
                            Logout
                        </Button>
                    ) : (
                        <Button
                            variant="contained"
                            color="primary"
                            component="a"
                            href={getLoginUrl()}
                        >
                            Login
                        </Button>
                    )}
                </Stack>
            </Paper>
        </Container>
    );
}
