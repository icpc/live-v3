import React from "react";
import { useNavigate } from "react-router-dom";
import { AppBar, Box, Button, Container, IconButton, Menu, MenuItem, Toolbar, Typography } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import PropTypes from "prop-types";

const pages = {
    "Oracle Locator": "oracleLocator",
    "Oracle Calibrator": "oracleCalibrator"
};


const ResponsiveAppBar = () => {
    const navigate = useNavigate();
    const [anchorElNav, setAnchorElNav] = React.useState(null);

    const handleOpenNavMenu = (event) => {
        setAnchorElNav(event.currentTarget);
    };

    const handleCloseNavMenu = () => {
        setAnchorElNav(null);
    };

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
                                {name}
                            </Button>)}
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
