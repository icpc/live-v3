import React from "react";
import "./App.css";
import AppNav from "./AppNav";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { SnackbarProvider } from "notistack";
import SniperLocator from "./components/SniperLocator";
import Calibrator from "./components/Calibrator";


function App() {
    return (
        <BrowserRouter basename={process.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav/>
                    <Routes>
                        <Route path="/" element={<SniperLocator/>}/>
                        <Route path="/sniperLocator" element={<SniperLocator/>}/>
                        <Route path="/sniperCalibrator" element={<Calibrator/>}/>
                    </Routes>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
