import React, { useEffect, useState } from "react";
import "./App.css";
import AppNav from "./AppNav";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { SnackbarProvider } from "notistack";
import { createApiGet, setFavicon, isShouldUseDarkColor, useLocalStorageState } from "./utils";
import { createTheme, ThemeProvider } from "@mui/material";
import { BACKEND_ROOT } from "./config";
import { faviconTemplate } from "./styles";
import SniperLocator from "./components/SniperLocator";

const getTheme = (contestColor) => {
    if (!contestColor) {
        return createTheme({
            palette: {
                text: {
                    primary: "#FFFFFF"
                }
            },
        });
    }
    const textColor = isShouldUseDarkColor(contestColor) ? "#000000" : "#FFFFFF";
    return createTheme({
        palette: {
            type: "light",
            primary: {
                main: contestColor,
                contrastText: textColor,
            },
            text: {
                primary: textColor,
            }
        },
    });
};


function App() {
    const [isOverlayPreviewShown, setIsOverlayPreviewShown] = useLocalStorageState("OverlayPreviewShown", false);

    const [contestColor, setContestColor] = useState(null);

    useEffect(() => {
        createApiGet(BACKEND_ROOT)("/api/overlay/visualConfig.json")
            .then(c => {
                if (c["CONTEST_COLOR"]) {
                    setContestColor(c["CONTEST_COLOR"]);
                    setFavicon(faviconTemplate
                        .replaceAll("{CONTEST_COLOR}", c["CONTEST_COLOR"])
                        .replaceAll("{TEXT_COLOR}", isShouldUseDarkColor(contestColor) ? "#000000" : "#FFFFFF"));
                }
                if (c["CONTEST_CAPTION"]) {
                    document.title = c["CONTEST_CAPTION"] + " — ICPC Live 3 Admin";
                }
            });
    }, []);

    return (
        <BrowserRouter basename={process.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <ThemeProvider theme={getTheme(contestColor)}>
                        <AppNav showOrHideOverlayPerview={() => setIsOverlayPreviewShown(!isOverlayPreviewShown)}/>
                    </ThemeProvider>
                    <Routes>
                        <Route path="/" element={<SniperLocator/>}/>
                    </Routes>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
