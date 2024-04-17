import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Container from "@mui/material/Container";
import Button from "@mui/material/Button";
import { IconButton, Menu, MenuItem } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import PreviewIcon from "@mui/icons-material/Preview";
import NotesIcon from "@mui/icons-material/Notes";
import PropTypes from "prop-types";
import { createApiGet } from "shared-code/utils";
import { BACKEND_ROOT } from "./config";

const defaultPages = {
    "Controls": "controls",
    // "Advertisement": "advertisement",
    "Titles": "titles",
    // "Title": "title",
    // "Picture": "picture",
    "TeamView": "teamview",
    "Scoreboard": "scoreboard",
    "Ticker": "ticker",
    "Dashboard": "dashboard",
    "Analytics": "analytics",
    "Spotlight": "teamSpotlight",
    "Advanced": "advancedJson",
    "Media": "media",
    "Backend Log": "log",
};


const ResponsiveAppBar = ({ showOrHideOverlayPerview }) => {
    const navigate = useNavigate();
    const [anchorElNav, setAnchorElNav] = React.useState(null);

    const handleOpenNavMenu = (event) => {
        setAnchorElNav(event.currentTarget);
    };

    const handleCloseNavMenu = () => {
        setAnchorElNav(null);
    };

    const [pages, setPages] = useState(defaultPages);

    useEffect(() => {
        createApiGet(BACKEND_ROOT)("/api/overlay/visualConfig.json")
            .then(c => {
                if (!c["ADMIN_HIDE_MENU"]) {
                    return ;
                }
                const filtredPages = {};
                Object.entries(defaultPages)
                    .filter(([k]) => !c["ADMIN_HIDE_MENU"].includes(k))
                    .forEach(([k, v]) => filtredPages[k] = v);
                console.log("applyHidePagesViaVisual", filtredPages);
                setPages(filtredPages);
            });
    }, []);

    return (
        <AppBar position="static">
            <Container maxWidth="xl">
                <Toolbar disableGutters>
                    <Box sx={{ flexGrow: 1, display: { xs: "flex", md: "none" } }}>
                        <IconButton size="large" aria-label="account of current user"
                            aria-controls="menu-appbar" aria-haspopup="true"
                            onClick={handleOpenNavMenu} color="inherit"><MenuIcon/></IconButton>
                        <Menu id="menu-appbar" anchorEl={anchorElNav}
                            anchorOrigin={{
                                vertical: "bottom",
                                horizontal: "left",
                            }} keepMounted
                            transformOrigin={{
                                vertical: "top",
                                horizontal: "left",
                            }} open={Boolean(anchorElNav)}
                            onClose={handleCloseNavMenu}
                            sx={{ display: { xs: "block", md: "none" } }}>
                            {Object.entries(pages).map(([name, url]) => (
                                <MenuItem key={url} onClick={() => {
                                    navigate(url);
                                    handleCloseNavMenu();
                                }}>
                                    <Typography textAlign="center" color={"black"}>{name}</Typography>
                                </MenuItem>
                            ))}
                        </Menu>
                    </Box>
                    <Typography variant="h6" noWrap component="div" onClick={() => navigate("/")}
                        sx={{ flexGrow: 1, display: { xs: "flex", md: "none" }, color: "text.primary" }}>
                        ICPC Live 3
                    </Typography>

                    <Box sx={{ mr: 2, display: { xs: "none", md: "flex" } }}>
                        <Typography variant="h5" noWrap onClick={() => navigate("/")}
                            sx={{ mr: 2, display: { xs: "none", md: "flex" }, color: "text.primary" }}>
                            ICPC Live 3
                        </Typography>
                    </Box>
                    <Box sx={{ flexGrow: 1, display: { xs: "none", md: "flex" } }}>
                        {Object.entries(pages).map(([name, url]) =>
                            <Button key={url} onClick={() => navigate(url)}
                                sx={{ my: 2, color: "text.primary", display: "block" }}>
                                {name === "Backend Log" ? <NotesIcon/> : name}
                            </Button>)}
                        <Button onClick={showOrHideOverlayPerview} sx={{ my: 2, color: "text.primary", display: "block" }}>
                            <PreviewIcon/>
                        </Button>
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
};
ResponsiveAppBar.propTypes = {
    showOrHideOverlayPerview: PropTypes.func.isRequired,
};


export default ResponsiveAppBar;
