import * as React from "react";
import { useNavigate } from "react-router-dom";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Container from "@mui/material/Container";
import Button from "@mui/material/Button";

const pages = {
    "Controls": "controls",
    "Advertisement": "advertisement",
    "Picture": "picture",
    "TeamView": "teamview",
    "Scoreboard": "scoreboard",
    "Ticker": "ticker",
};


const ResponsiveAppBar = () => {
    const navigate = useNavigate();
    // const [ , setAnchorElNav] = React.useState(null);
    // const [ , setAnchorElUser] = React.useState(null);

    // const handleOpenNavMenu = (event) => {
    //     setAnchorElNav(event.currentTarget);
    // };
    // const handleOpenUserMenu = (event) => {
    //     setAnchorElUser(event.currentTarget);
    // };
    //
    // const handleCloseNavMenu = () => {
    //     setAnchorElNav(null);
    // };
    //
    // const handleCloseUserMenu = () => {
    //     setAnchorElUser(null);
    // };

    return (
        <AppBar position="static">
            <Container maxWidth="xl">
                <Toolbar disableGutters>
                    <Box>
                        <Typography
                            variant="h5"
                            noWrap
                            onClick={() => navigate("/")}
                            sx={{ mr: 2, display: { md: "flex" } }}
                        >
                            ICPC Live 3
                        </Typography>
                    </Box>
                    <Box sx={{ flexGrow: 1, display: { xs: "flex" } }}>
                        {Object.entries(pages).map(([name, url]) => <Button
                            key={url}
                            onClick={() => navigate(url)}
                            sx={{ my: 2, color: "white", display: "block" }}
                        >
                            {name}
                        </Button>)}
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
};
export default ResponsiveAppBar;
