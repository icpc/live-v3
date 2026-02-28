import { AdminLayout } from "./AdminLayout";
import { Box, Typography, Link, List, ListItem, Paper } from "@mui/material";
import { useEffect, useState } from "react";

const GitInfoItem = ({ label, url }: { label: string; url: string }) => {
    const [info, setInfo] = useState("loading...");

    useEffect(() => {
        fetch(url)
            .then((response) => response.text())
            .then((text) => setInfo(text))
            .catch((error) => {
                console.error(`Error fetching ${label}:`, error);
                setInfo("error");
            });
    }, [url, label]);

    return (
        <Typography variant="body1">
            <strong>{label}:</strong> {info}
        </Typography>
    );
};

const App = () => {
    return (
        <AdminLayout>
            <Box sx={{ p: 3, display: "flex", flexDirection: "column", alignItems: "center", gap: 3 }}>
                <Typography variant="h4">ICPC Live 3</Typography>
                
                <Paper sx={{ p: 2, minWidth: 300 }}>
                    <GitInfoItem label="Commit" url="/git_commit" />
                    <GitInfoItem label="Branch" url="/git_branch" />
                    <GitInfoItem label="Version" url="/git_description" />
                </Paper>

                <Box sx={{ textAlign: "center" }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>Useful Links</Typography>
                    <List sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="/admin" variant="h5" underline="hover">/admin</Link>
                        </ListItem>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="/admin-contest-info/contestInfo" variant="h5" underline="hover">/admin-contest-info/contestInfo</Link>
                        </ListItem>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="/admin-contest-info/advancedJson" variant="h5" underline="hover">/admin-contest-info/advancedJson</Link>
                        </ListItem>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="/overlay?noStatus" variant="h5" underline="hover">/overlay?noStatus</Link>
                        </ListItem>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="/api/admin/advancedJsonPreview?fields=all" variant="h5" underline="hover">
                                /api/admin/advancedJsonPreview?fields=all
                            </Link>
                        </ListItem>
                        <ListItem sx={{ justifyContent: "center" }}>
                            <Link href="https://github.com/icpc/live-v3" variant="h5" underline="hover" target="_blank" rel="noopener">
                                https://github.com/icpc/live-v3
                            </Link>
                        </ListItem>
                    </List>
                </Box>
            </Box>
        </AdminLayout>
    );
};

export default App;
