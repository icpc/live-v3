import React, { useState } from "react";
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
import { SnackbarProvider } from "notistack";
import ScoreboardSettings from "./components/ScoreboardSettings";
import Dashboard from "./components/Dashboard";

function App() {
    const [isOverlayPreviewShown, setIsOverlayPreviewShown] = useState(false);
    return (
        <BrowserRouter basename={process.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav showOrHideOverlayPerview={() => setIsOverlayPreviewShown(state => !state)}/>
                    <Routes>
                        <Route path="/" element={<Controls/>}/>
                        <Route path="/controls" element={<Controls/>}/>
                        <Route path="/advertisement" element={<Advertisement/>}/>
                        <Route path="/title" element={<Title/>}/>
                        <Route path="/picture" element={<Picture/>}/>
                        <Route path="/teamview" element={<TeamView/>}/>
                        <Route path="/teampvp" element={<TeamPVP/>}/>
                        <Route path="/scoreboard" element={<ScoreboardSettings/>}/>
                        <Route path="/ticker" element={<TickerMessage/>}/>
                        <Route path="/dashboard" element={<Dashboard/>}/>
                        {/* <Route path="/advancedproperties" element={<AdvancedProperties/>}/> */}
                    </Routes>
                    <Overlay isOverlayPreviewShown={isOverlayPreviewShown}/>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
