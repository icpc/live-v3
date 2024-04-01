import React, { useEffect, useState } from "react";
import "./App.css";
import AppNav from "./AppNav";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Overlay } from "./components/Overlay";
import TickerMessage from "./components/TickerMessage";
import Controls from "./components/Controls";
import Advertisement from "./components/Advertisement";
import Title from "./components/Title";
import Picture from "./components/Picture";
import TeamView from "./components/TeamView";
import { SnackbarProvider } from "notistack";
import ScoreboardManager from "./components/ScoreboardManager";
import BackendLog from "./components/BackendLog";
import Dashboard from "./components/Dashboard";
import Analytics from "./components/Analytics";
import TeamSpotlight from "./components/TeamSpotlight";
import { createApiGet } from "shared-code/utils";
import { setFavicon, isShouldUseDarkColor, useLocalStorageState } from "./utils";
import FullScreenClockManager from "./components/FullScreenClockManager";
import AdvancedJson from "./components/AdvancedJson";
import MediaFiles from "./components/MediaFiles";
import { createTheme, ThemeProvider } from "@mui/material";
import { BACKEND_ROOT } from "./config";
import { faviconTemplate } from "./styles";

const dashboard_elements = {
    "Controls": <Controls/>,
    "Advertisement": <Advertisement/>,
    "Title": <Title/>,
    "Picture": <Picture/>,
    "Scoreboard": <ScoreboardManager/>,
    "Ticker": <TickerMessage/>,
    "Full screen clock": <FullScreenClockManager/>,
};

const title_elements = {
    "Advertisement": <Advertisement/>,
    "Title": <Title/>,
    "Picture": <Picture/>,
};

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
                        <Route path="/" element={<Controls/>}/>
                        <Route path="/controls" element={<Controls/>}/>
                        {/* <Route path="/advertisement" element={<Advertisement/>}/> */}
                        {/* <Route path="/title" element={<Title/>}/> */}
                        <Route path="/titles"
                            element={<Dashboard elements={title_elements} layout="oneColumn" maxWidth="lg"/>}/>
                        {/* <Route path="/picture" element={<Picture/>}/> */}
                        <Route path="/teamview" element={<TeamView/>}/>
                        {/*<Route path="/teampvp" element={<TeamPVP/>}/>*/}
                        {/*<Route path="/splitscreen" element={<SplitScreen/>}/>*/}
                        <Route path="/scoreboard" element={<ScoreboardManager/>}/>
                        <Route path="/ticker" element={<TickerMessage/>}/>
                        <Route path="/dashboard" element={<Dashboard elements={dashboard_elements}/>}/>
                        <Route path="/log" element={<BackendLog/>}/>
                        <Route path="/analytics" element={<Analytics/>}/>
                        <Route path="/teamSpotlight" element={<TeamSpotlight/>}/>
                        <Route path="/advancedJson" element={<AdvancedJson/>}/>
                        <Route path="/media" element={<MediaFiles/>}/>
                    </Routes>
                    <Overlay isOverlayPreviewShown={isOverlayPreviewShown}/>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
