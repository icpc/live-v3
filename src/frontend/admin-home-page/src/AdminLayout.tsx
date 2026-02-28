import React, { useState } from "react";
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
import { useLocation, useNavigate } from "react-router-dom";

import { MenuItem } from "./menuConfig";

export const AdminLayout = ({
    children,
    hideToggleButton,
    drawerOpen,
    onDrawerToggle,
    menuItems,
    onNavigate,
}: {
    children: React.ReactNode;
    hideToggleButton?: boolean;
    drawerOpen?: boolean;
    onDrawerToggle?: () => void;
    menuItems?: MenuItem[];
    onNavigate?: (path: string) => void;
}) => {
    const location = useLocation();
    const navigate = useNavigate();
    const isRoot = location.pathname === "/" || location.pathname === "";
    const [localDrawerOpen, setLocalDrawerOpen] = useState(isRoot);

    const actualDrawerOpen =
        drawerOpen !== undefined ? drawerOpen : localDrawerOpen;
    const handleDrawerToggle =
        onDrawerToggle ||
        (() => {
            setLocalDrawerOpen(!localDrawerOpen);
        });

    const handleMenuItemClick = (path: string) => {
        if (onNavigate) {
            onNavigate(path);
            return;
        }

        navigate(path);
    };

    const drawerWidth = 250;

    return (
        <Box sx={{ display: "flex", width: "100%", minHeight: "100vh" }}>
            {!hideToggleButton && !isRoot && (
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
            )}
            <Drawer
                variant={isRoot ? "permanent" : "temporary"}
                anchor="left"
                open={actualDrawerOpen}
                onClose={() =>
                    onDrawerToggle
                        ? onDrawerToggle()
                        : setLocalDrawerOpen(false)
                }
                sx={{
                    width: isRoot ? drawerWidth : 0,
                    flexShrink: 0,
                    "& .MuiDrawer-paper": {
                        width: drawerWidth,
                        boxSizing: "border-box",
                    },
                }}
            >
                <Box sx={{ width: drawerWidth }} role="presentation">
                    <List>
                        {menuItems.map((item) => (
                            <ListItem key={item.name} disablePadding>
                                <ListItemButton
                                    onClick={() =>
                                        handleMenuItemClick(item.path)
                                    }
                                    selected={
                                        location.pathname === item.path ||
                                        (item.path !== "/" &&
                                            location.pathname.startsWith(
                                                item.path,
                                            ))
                                    }
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
                    width: isRoot ? `calc(100% - ${drawerWidth}px)` : "100%",
                }}
            >
                {children}
            </Box>
        </Box>
    );
};
