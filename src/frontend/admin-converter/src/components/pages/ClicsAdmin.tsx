import { useState } from "react";
import {
    Typography,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Paper,
    Container,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Box,
} from "@mui/material";

const FEED_VERSIONS = [
    { label: "2020_03", value: "2020_03/" },
    { label: "2022_07", value: "2022_07/" },
    { label: "2023_06", value: "2023_06/" },
    { label: "2026_01", value: "2026_01/" },
    { label: "DRAFT", value: "" },
];

export function ClicsAdmin() {
    const [version, setVersion] = useState("2023_06/");

    const prefix = `/clics/${version}api`;

    return (
        <Container maxWidth="md" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>
                    Clics Exporter
                </Typography>

                <Box sx={{ mb: 3, maxWidth: 200 }}>
                    <FormControl fullWidth size="small">
                        <InputLabel id="version-select-label">
                            Feed Version
                        </InputLabel>
                        <Select
                            labelId="version-select-label"
                            id="version-select"
                            value={version}
                            label="Feed Version"
                            onChange={(e) => setVersion(e.target.value)}
                        >
                            {FEED_VERSIONS.map((v) => (
                                <MenuItem key={v.label} value={v.value}>
                                    {v.label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Box>

                <Typography variant="body1" paragraph>
                    Manage and access CLICS-compatible data feeds.
                </Typography>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton component="a" href={prefix}>
                            <ListItemText
                                primary="Clics API root"
                                secondary="Browse the CLICS API"
                            />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            component="a"
                            href={`${prefix}/contests/contest`}
                        >
                            <ListItemText
                                primary="Clics contest API root"
                                secondary="Direct access to contest data"
                            />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            component="a"
                            href={`${prefix}/contests/contest/event-feed`}
                        >
                            <ListItemText
                                primary="Clics event feed"
                                secondary="Real-time event stream"
                            />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            component="a"
                            href={
                                version === ""
                                    ? "/clics/api/archive.zip"
                                    : `/clics/${version}api/archive.zip`
                            }
                        >
                            <ListItemText
                                primary="CLICS contest archive (zip)"
                                secondary="Download full contest data as ZIP"
                            />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Paper>
        </Container>
    );
}
