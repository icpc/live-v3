import React from "react";
import "./App.css";

import AppNav from "./AppNav";
import Container from "@mui/material/Container";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Ticker from "./Ticker";
import Controls from "./Controls";
import Advertisement from "./Advertisement";


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
                        <Route path = "/ticker" element={<Ticker/>}/>
                    </Routes>
                </Container>
            </div>
        </BrowserRouter>
    );
}

export default App;
