import { useEffect, useState, useCallback } from "react";
import "./App.css";
import AppNav from "./AppNav.tsx";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Overlay } from "./components/Overlay.tsx";
import TickerMessage from "./components/TickerMessage.tsx";
import ControlsPage from "./components/pages/ControlsPage.tsx";
import Advertisement from "./components/Advertisement.tsx";
import Title from "./components/Title";
import Picture from "./components/Picture.tsx";
import TeamView from "./components/pages/TeamView";
import ContestLog from "./components/pages/ContestInfo";
import { SnackbarProvider } from "notistack";
import BackendLog from "./components/BackendLog.tsx";
import Dashboard from "./components/Dashboard.tsx";
import Analytics from "@/components/pages/Analytics";
import TeamSpotlight from "./components/TeamSpotlight.tsx";
import { createApiGet } from "@shared/utils";
import {
    setFavicon,
    isShouldUseDarkColor,
    useLocalStorageState,
} from "./utils";
import AdvancedJson from "./components/AdvancedJson.tsx";
import MediaFiles from "./components/MediaFiles.tsx";
import { createTheme, ThemeProvider } from "@mui/material";
import { BACKEND_ROOT } from "./config";
import { faviconTemplate } from "./styles.ts";
import {
    ReloadHandleContext,
    useReloadHandleService,
} from "@/services/reloadHandler.ts";
import ScoreboardPage from "@/components/pages/ScoreboardPage.tsx";

const title_elements = {
    Advertisement: <Advertisement />,
    Title: <Title />,
    Picture: <Picture />,
};

const getTheme = (contestColor) => {
    if (!contestColor) {
        return createTheme({
            palette: {
                text: {
                    primary: "#FFFFFF",
                },
            },
        });
    }
    const textColor = isShouldUseDarkColor(contestColor)
        ? "#000000"
        : "#FFFFFF";
    return createTheme({
        palette: {
            mode: "light",
            primary: {
                main: contestColor,
                contrastText: textColor,
            },
            text: {
                primary: textColor,
            },
        },
    });
};

function App() {
    const [isOverlayPreviewShown, setIsOverlayPreviewShown] =
        useLocalStorageState("OverlayPreviewShown", false);

    const [contestColor, setContestColor] = useState(null);

    useEffect(() => {
        createApiGet(BACKEND_ROOT)("/api/overlay/visualConfig.json").then(
            (c) => {
                if (c["CONTEST_COLOR"]) {
                    setContestColor(c["CONTEST_COLOR"]);
                    setFavicon(
                        faviconTemplate
                            .replaceAll("{CONTEST_COLOR}", c["CONTEST_COLOR"])
                            .replaceAll(
                                "{TEXT_COLOR}",
                                isShouldUseDarkColor(contestColor)
                                    ? "#000000"
                                    : "#FFFFFF",
                            ),
                    );
                }
                if (c["CONTEST_CAPTION"]) {
                    document.title =
                        c["CONTEST_CAPTION"] + " — ICPC Live 3 Admin";
                }
            },
        );
    }, []);

    const reloadHandleService = useReloadHandleService();

    const handleToggleOverlayPreview = useCallback(() => {
        setIsOverlayPreviewShown(!isOverlayPreviewShown);
    }, [isOverlayPreviewShown, setIsOverlayPreviewShown]);

    return (
        <BrowserRouter basename={import.meta.env.BASE_URL ?? ""}>
            <ReloadHandleContext.Provider value={reloadHandleService}>
                <SnackbarProvider maxSnack={5}>
                    <div className="App">
                        <ThemeProvider theme={getTheme(contestColor)}>
                            <AppNav
                                showOrHideOverlayPreview={
                                    handleToggleOverlayPreview
                                }
                            />
                        </ThemeProvider>
                        <Routes>
                            <Route path="/" element={<ControlsPage />} />
                            <Route
                                path="/controls"
                                element={<ControlsPage />}
                            />
                            {/* <Route path="/advertisement" element={<Advertisement/>}/> */}
                            {/* <Route path="/title" element={<Title/>}/> */}
                            <Route
                                path="/titles"
                                element={
                                    <Dashboard
                                        elements={title_elements}
                                        layout="oneColumn"
                                        maxWidth="lg"
                                    />
                                }
                            />
                            {/* <Route path="/picture" element={<Picture/>}/> */}
                            <Route path="/teamview" element={<TeamView />} />
                            {/*<Route path="/teampvp" element={<TeamPVP/>}/>*/}
                            {/*<Route path="/splitscreen" element={<SplitScreen/>}/>*/}
                            <Route
                                path="/scoreboard"
                                element={<ScoreboardPage />}
                            />
                            <Route path="/ticker" element={<TickerMessage />} />
                            <Route path="/log" element={<BackendLog />} />
                            <Route
                                path="/contestInfo"
                                element={<ContestLog />}
                            />
                            <Route path="/analytics" element={<Analytics />} />
                            <Route
                                path="/teamSpotlight"
                                element={<TeamSpotlight />}
                            />
                            <Route
                                path="/advancedJson"
                                element={<AdvancedJson />}
                            />
                            <Route path="/media" element={<MediaFiles />} />
                        </Routes>
                        <Overlay
                            isOverlayPreviewShown={isOverlayPreviewShown}
                        />
                    </div>
                </SnackbarProvider>
            </ReloadHandleContext.Provider>
        </BrowserRouter>
    );
}

export default App;
