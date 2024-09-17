import React from "react";
import "./App.css";
import AppNav from "./AppNav";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { SnackbarProvider } from "notistack";
import OracleLocator from "./components/OracleLocator";
import Calibrator from "./components/Calibrator.jsx";


function App() {
    return (
        <BrowserRouter basename={import.meta.env.PUBLIC_URL ?? ""}>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav/>
                    <Routes>
                        <Route path="/" element={<OracleLocator/>}/>
                        <Route path="/locator" element={<OracleLocator/>}/>
                        <Route path="/oracleCalibrator" element={<Calibrator/>}/>
                    </Routes>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
