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
import { createApiGet } from "shared-code/utils";
import { BACKEND_ROOT } from "./config";

interface PageConfig {
    [pageName: string]: string;
}

interface ResponsiveAppBarProps {
    showOrHideOverlayPreview: () => void;
}

const DEFAULT_PAGES: PageConfig = {
    Controls: "controls",
    Titles: "titles",
    TeamView: "teamview",
    Scoreboard: "scoreboard",
    Ticker: "ticker",
    Analytics: "analytics",
    Spotlight: "teamSpotlight",
    Advanced: "advancedJson",
    Media: "media",
    Info: "contestInfo",
    "Backend Log": "log",
} as const;

function filterVisiblePages(
    defaultPages: PageConfig,
    hiddenPageNames: string[],
): PageConfig {
    const filteredPages: PageConfig = {};

    Object.entries(defaultPages)
        .filter(([pageName]) => !hiddenPageNames.includes(pageName))
        .forEach(([pageName, route]) => {
            filteredPages[pageName] = route;
        });

    return filteredPages;
}

// TODO: types for createApiGet and other staff
async function loadVisualConfig() {
    try {
        return await createApiGet(BACKEND_ROOT)(
            "/api/overlay/visualConfig.json",
        );
    } catch (error) {
        console.error(
            `Failed to load visual config, using default pages: ${error}`,
        );
        return {};
    }
}

function ResponsiveAppBar({
    showOrHideOverlayPreview,
}: ResponsiveAppBarProps): React.ReactElement {
    const navigate = useNavigate();
    const [anchorElNav, setAnchorElNav] = useState<HTMLElement | null>(null);
    const [pages, setPages] = useState<PageConfig>(DEFAULT_PAGES);

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

    useEffect(() => {
        const loadConfig = async () => {
            try {
                const config = await loadVisualConfig();

                if (
                    config.ADMIN_HIDE_MENU &&
                    config.ADMIN_HIDE_MENU.length > 0
                ) {
                    const filteredPages = filterVisiblePages(
                        DEFAULT_PAGES,
                        config.ADMIN_HIDE_MENU,
                    );
                    console.log(
                        "Applied hidden pages via visual config:",
                        filteredPages,
                    );
                    setPages(filteredPages);
                }
            } catch (error) {
                console.error("Failed to apply visual config:", error);
            }
        };

        loadConfig();
    }, []);

    const renderPageButton = (pageName: string, route: string) => (
        <Button
            key={route}
            onClick={() => handleNavigate(route)}
            sx={{ my: 2, color: "text.primary", display: "block" }}
        >
            {pageName === "Backend Log" ? <NotesIcon /> : pageName}
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
                            {Object.entries(pages).map(([pageName, route]) =>
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
                        {Object.entries(pages).map(([pageName, route]) =>
                            renderPageButton(pageName, route as string),
                        )}
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
