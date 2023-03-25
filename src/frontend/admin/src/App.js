import React from "react";
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
import TeamPVP from "./components/TeamPVP";
import SplitScreen from "./components/SplitScreen";
import { SnackbarProvider } from "notistack";
import ScoreboardManager from "./components/ScoreboardManager";
import BackendLog from "./components/BackendLog";
import Dashboard from "./components/Dashboard";
import Analytics from "./components/Analytics";
import TeamSpotlight from "./components/TeamSpotlight";
import { useLocalStorageState } from "./utils";
import FullScreenClockManager from "./components/FullScreenClockManager";

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


function App() {
    const [isOverlayPreviewShown, setIsOverlayPreviewShown] = useLocalStorageState("OverlayPreviewShown", false);
    return (
        <BrowserRouter basename={process.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav showOrHideOverlayPerview={() => setIsOverlayPreviewShown(!isOverlayPreviewShown)}/>
                    <Routes>
                        <Route path="/" element={<Controls/>}/>
                        <Route path="/controls" element={<Controls/>}/>
                        {/* <Route path="/advertisement" element={<Advertisement/>}/> */}
                        {/* <Route path="/title" element={<Title/>}/> */}
                        <Route path="/titles" element={<Dashboard elements={title_elements} layout="oneColumn" maxWidth="lg"/>}/>
                        {/* <Route path="/picture" element={<Picture/>}/> */}
                        <Route path="/teamview" element={<TeamView/>}/>
                        <Route path="/teampvp" element={<TeamPVP/>}/>
                        <Route path="/splitscreen" element={<SplitScreen/>}/>
                        <Route path="/scoreboard" element={<ScoreboardManager/>}/>
                        <Route path="/ticker" element={<TickerMessage/>}/>
                        <Route path="/dashboard" element={<Dashboard elements={dashboard_elements}/>}/>
                        <Route path="/log" element={<BackendLog/>}/>
                        <Route path="/analytics" element={<Analytics/>}/>
                        <Route path="/teamSpotlight" element={<TeamSpotlight/>}/>
                        {/* <Route path="/advancedproperties" element={<AdvancedProperties/>}/> */}
                    </Routes>
                    <Overlay isOverlayPreviewShown={isOverlayPreviewShown}/>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
