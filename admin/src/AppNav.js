import * as React from "react";
import { useNavigate } from "react-router-dom";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Container from "@mui/material/Container";
import Button from "@mui/material/Button";
import { IconButton, Menu, MenuItem } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";

const pages = {
    "Controls": "controls",
    "Advertisement": "advertisement",
    "Picture": "picture",
    "TeamView": "teamview",
    "Scoreboard": "scoreboard",
    "Ticker": "ticker",
    "Advanced Properties": "AdvancedProperties",
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
                                    <Typography textAlign="center">{name}</Typography>
                                </MenuItem>
                            ))}
                        </Menu>
                    </Box>
                    <Typography variant="h6" noWrap component="div" onClick={() => navigate("/")}
                        sx={{ flexGrow: 1, display: { xs: "flex", md: "none" } }}>
                        ICPC Live 3
                    </Typography>

                    <Box sx={{ mr: 2, display: { xs: "none", md: "flex" } }}>
                        <Typography variant="h5" noWrap onClick={() => navigate("/")}
                            sx={{ mr: 2, display: { xs: "none", md: "flex" } }}>
                            ICPC Live 3
                        </Typography>
                    </Box>
                    <Box sx={{ flexGrow: 1, display: { xs: "none", md: "flex" } }}>
                        {Object.entries(pages).map(([name, url]) =>
                            <Button key={url} onClick={() => navigate(url)}
                                sx={{ my: 2, color: "white", display: "block" }}>
                                {name}
                            </Button>)}
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
};
export default ResponsiveAppBar;
