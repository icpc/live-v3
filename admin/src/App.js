import React from "react";
import "./App.css";
import AppNav from "./AppNav";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import TickerMessage from "./components/TickerMessage";
import Controls from "./components/Controls";
import Advertisement from "./components/Advertisement";
import Picture from "./components/Picture";
import TeamView from "./components/TeamView";
import { SnackbarProvider } from "notistack";
import ScoreboardSettings from "./components/ScoreboardSettings";
import Dashboard from "./components/Dashboard";

function App() {
    return (
        <BrowserRouter basename={process.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav/>
                    <Routes>
                        <Route path="/" element={<Controls/>}/>
                        <Route path="/controls" element={<Controls/>}/>
                        <Route path="/advertisement" element={<Advertisement/>}/>
                        <Route path="/picture" element={<Picture/>}/>
                        <Route path="/teamview" element={<TeamView/>}/>
                        <Route path="/scoreboard" element={<ScoreboardSettings/>}/>
                        <Route path="/ticker" element={<TickerMessage/>}/>
                        <Route path="/dashboard" element={<Dashboard/>}/>
                        {/* <Route path="/advancedproperties" element={<AdvancedProperties/>}/> */}
                    </Routes>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
