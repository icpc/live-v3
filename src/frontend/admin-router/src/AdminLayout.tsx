import React, { useEffect, useState } from "react";
import {
    Box,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";

import { MenuItem } from "./menuConfig";

export const AdminLayout = ({ children }: { children: React.ReactNode }) => {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [menuItems, setMenuItems] = useState<MenuItem[]>([]);

    useEffect(() => {
        fetch("/live-router/menu")
            .then((response) => response.json())
            .then((data) => setMenuItems(data))
            .catch((error) => console.error("Error fetching menu:", error));
    }, []);

    const handleDrawerToggle = () => {
        setDrawerOpen(!drawerOpen);
    };

    const handleMenuItemClick = (path: string) => {
        window.location.href = path;
    };

    return (
        <Box sx={{ display: "flex", width: "100%", minHeight: "100vh" }}>
            <IconButton
                color="inherit"
                aria-label="open drawer"
                onClick={handleDrawerToggle}
                sx={{
                    position: "fixed",
                    left: 16,
                    top: 100, // Slightly below the typical AppBar
                    zIndex: 1300,
                    backgroundColor: "rgba(0, 0, 0, 0.1)",
                    "&:hover": {
                        backgroundColor: "rgba(0, 0, 0, 0.2)",
                    },
                }}
            >
                <MenuIcon />
            </IconButton>
            <Drawer
                anchor="left"
                open={drawerOpen}
                onClose={() => setDrawerOpen(false)}
            >
                <Box
                    sx={{ width: 250 }}
                    role="presentation"
                >
                    <List>
                        {menuItems.map((item) => (
                            <ListItem key={item.name} disablePadding>
                                <ListItemButton
                                    onClick={() => handleMenuItemClick(item.path)}
                                    selected={window.location.pathname === item.path || (item.path !== "/" && window.location.pathname.startsWith(item.path))}
                                >
                                    <ListItemText primary={item.name} />
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </Drawer>
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    width: "100%",
                }}
            >
                {children}
            </Box>
        </Box>
    );
};
