import React, { useState, useEffect, useCallback } from "react";
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
import PreviewIcon from "@mui/icons-material/Preview";
import NotesIcon from "@mui/icons-material/Notes";
import { MenuItem as AdminMenuItem } from "admin-home-page";
import { getAdminOverlayMenuItems } from "./navigation";

interface ResponsiveAppBarProps {
    showOrHideOverlayPreview: () => void;
    onDrawerToggle: () => void;
    hiddenPageNames: string[];
}

function ResponsiveAppBar({
    showOrHideOverlayPreview,
    onDrawerToggle,
    hiddenPageNames,
}: ResponsiveAppBarProps): React.ReactElement {
    const navigate = useNavigate();
    const [anchorElNav, setAnchorElNav] = useState<HTMLElement | null>(null);
    const [pages, setPages] = useState<AdminMenuItem[]>([]);

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

    const applyVisiblePages = useCallback((hiddenPages: string[]) => {
        setPages(getAdminOverlayMenuItems(hiddenPages));
    }, []);

    useEffect(() => {
        applyVisiblePages(hiddenPageNames);
    }, [applyVisiblePages, hiddenPageNames]);

    const renderPageButton = (page: AdminMenuItem) => (
        <Button
            key={page.path}
            onClick={() => handleNavigate(page.path)}
            sx={{ my: 2, color: "text.primary", display: "block" }}
        >
            {page.name === "Backend Log" ? <NotesIcon /> : page.name}
        </Button>
    );

    const renderMenuItem = (page: AdminMenuItem) => (
        <MenuItem key={page.path} onClick={() => handleNavigate(page.path)}>
            <Typography textAlign="center" color="black">
                {page.name}
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
                            {pages.map((page) => renderMenuItem(page))}
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
                        ICPC Live 3
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
                            ICPC Live 3
                        </Typography>
                    </Box>

                    <Box
                        sx={{
                            flexGrow: 1,
                            display: { xs: "none", md: "flex" },
                        }}
                    >
                        {pages.map((page) => renderPageButton(page))}
                        <Button
                            onClick={showOrHideOverlayPreview}
                            sx={{
                                my: 2,
                                color: "text.primary",
                                display: "block",
                            }}
                            aria-label="Toggle overlay preview"
                        >
                            <PreviewIcon />
                        </Button>
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
}

export default ResponsiveAppBar;
