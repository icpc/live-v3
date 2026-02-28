import { AdminLayout } from "./AdminLayout";
import { Box, Typography, Link, List, ListItem, Paper } from "@mui/material";
import { useEffect, useState } from "react";

const App = () => {
    const [gitInfo, setGitInfo] = useState<{ commit: string, branch: string, description: string } | null>(null);
    const [usefulLinks, setUsefulLinks] = useState<{ name: string, url: string }[]>([]);

    useEffect(() => {
        fetch("/live-router/git")
            .then((response) => response.json())
            .then((data) => setGitInfo(data))
            .catch((error) => console.error("Error fetching git info:", error));

        fetch("/live-router/links")
            .then((response) => response.json())
            .then((data) => setUsefulLinks(data))
            .catch((error) => console.error("Error fetching links:", error));
    }, []);

    return (
        <AdminLayout>
            <Box sx={{ p: 3, display: "flex", flexDirection: "column", alignItems: "center", gap: 3 }}>
                <Typography variant="h4">ICPC Live 3</Typography>
                
                <Paper sx={{ p: 2, minWidth: 300 }}>
                    <Typography variant="body1">
                        <strong>Commit:</strong> {gitInfo?.commit || "loading..."}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Branch:</strong> {gitInfo?.branch || "loading..."}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Version:</strong> {gitInfo?.description || "loading..."}
                    </Typography>
                </Paper>

                <Box sx={{ textAlign: "center" }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>Useful Links</Typography>
                    <List sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                        {usefulLinks.map((link) => (
                            <ListItem key={link.url} sx={{ justifyContent: "center" }}>
                                <Link href={link.url} variant="h5" underline="hover" {...(link.url.startsWith("http") ? { target: "_blank", rel: "noopener" } : {})}>
                                    {link.name}
                                </Link>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </Box>
        </AdminLayout>
    );
};

export default App;
