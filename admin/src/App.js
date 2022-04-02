import React from "react";
import "./App.css";

import AppNav from "./AppNav";
import Container from "@mui/material/Container";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Ticker from "./components/Ticker";
import Controls from "./components/Controls";
import Advertisement from "./components/Advertisement";
import Picture from "./components/Picture";


function App() {
    return (
        <BrowserRouter>
            <div className="App">
                <AppNav/>
                <Container maxWidth="l" sx={{ pt: 2 }}>
                    <Routes>
                        <Route path = "/" element={<Controls/>}/>
                        <Route path = "/controls" element={<Controls/>}/>
                        <Route path = "/advertisement" element={<Advertisement/>}/>
                        <Route path = "/picture" element={<Picture/>}/>
                        <Route path = "/ticker" element={<Ticker/>}/>
                    </Routes>
                </Container>
            </div>
        </BrowserRouter>
    );
}

export default App;
