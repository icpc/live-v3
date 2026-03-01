import React, { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
    AppBar,
    Box,
    Toolbar,
    Typography,
    Container,
    Button,
    IconButton,
    Menu,
    MenuItem,
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";

interface PageConfig {
    [pageName: string]: string;
}

interface ResponsiveAppBarProps {
    onDrawerToggle: () => void;
}

const DEFAULT_PAGES: PageConfig = {
    "Clics": "/clics",
    "PCMS": "/pcms",
    "ICPC": "/icpc",
    "Reactions": "/reactions",
    "Login/Logout": "/session",
} as const;

function ResponsiveAppBar({ onDrawerToggle }: ResponsiveAppBarProps): React.ReactElement {
    const navigate = useNavigate();
    const [anchorElNav, setAnchorElNav] = useState<HTMLElement | null>(null);

    const handleOpenNavMenu = useCallback(
        (event: React.MouseEvent<HTMLElement>) => {
            setAnchorElNav(event.currentTarget);
        },
        [],
    );

    const handleCloseNavMenu = useCallback(() => {
        setAnchorElNav(null);
    }, []);

    const handleNavigate = useCallback(
        (route: string) => {
            navigate(route);
            handleCloseNavMenu();
        },
        [navigate, handleCloseNavMenu],
    );

    const handleHomeNavigate = useCallback(() => {
        navigate("/");
    }, [navigate]);

    const renderPageButton = (pageName: string, route: string) => (
        <Button
            key={route}
            onClick={() => handleNavigate(route)}
            sx={{ my: 2, color: "text.primary", display: "block" }}
        >
            {pageName}
        </Button>
    );

    const renderMenuItem = (pageName: string, route: string) => (
        <MenuItem key={route} onClick={() => handleNavigate(route)}>
            <Typography textAlign="center" color="black">
                {pageName}
            </Typography>
        </MenuItem>
    );

    return (
        <AppBar position="static">
            <Container maxWidth="xl">
                <Toolbar disableGutters>
                    <IconButton
                        size="large"
                        aria-label="side menu"
                        onClick={onDrawerToggle}
                        color="inherit"
                        sx={{ mr: 1 }}
                    >
                        <MenuIcon />
                    </IconButton>
                    <Box
                        sx={{
                            flexGrow: 1,
                            display: { xs: "flex", md: "none" },
                        }}
                    >
                        <IconButton
                            size="large"
                            aria-label="navigation menu"
                            aria-controls="menu-appbar"
                            aria-haspopup="true"
                            onClick={handleOpenNavMenu}
                            color="inherit"
                        >
                            <MenuIcon />
                        </IconButton>
                        <Menu
                            id="menu-appbar"
                            anchorEl={anchorElNav}
                            anchorOrigin={{
                                vertical: "bottom",
                                horizontal: "left",
                            }}
                            keepMounted
                            transformOrigin={{
                                vertical: "top",
                                horizontal: "left",
                            }}
                            open={Boolean(anchorElNav)}
                            onClose={handleCloseNavMenu}
                            sx={{ display: { xs: "block", md: "none" } }}
                        >
                            {Object.entries(DEFAULT_PAGES).map(([pageName, route]) =>
                                renderMenuItem(pageName, route as string),
                            )}
                        </Menu>
                    </Box>

                    <Typography
                        variant="h6"
                        noWrap
                        component="div"
                        onClick={handleHomeNavigate}
                        sx={{
                            flexGrow: 1,
                            display: { xs: "flex", md: "none" },
                            color: "text.primary",
                            cursor: "pointer",
                        }}
                    >
                        Converter Admin
                    </Typography>

                    <Box sx={{ mr: 2, display: { xs: "none", md: "flex" } }}>
                        <Typography
                            variant="h5"
                            noWrap
                            onClick={handleHomeNavigate}
                            sx={{
                                mr: 2,
                                display: { xs: "none", md: "flex" },
                                color: "text.primary",
                                cursor: "pointer",
                            }}
                        >
                            Converter Admin
                        </Typography>
                    </Box>

                    <Box
                        sx={{
                            flexGrow: 1,
                            display: { xs: "none", md: "flex" },
                        }}
                    >
                        {Object.entries(DEFAULT_PAGES).map(([pageName, route]) =>
                            renderPageButton(pageName, route as string),
                        )}
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
}

export default ResponsiveAppBar;
