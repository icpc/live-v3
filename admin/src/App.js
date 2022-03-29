import React from "react";
import "./App.css";

import Container from "@mui/material/Container";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import AppNav from "./AppNav";
import Controls from "./Controls";
import Advertisement from "./Advertisement";


function App() {
    return (
        <BrowserRouter>
            <div className="App">
                <AppNav/>
                <Container maxWidth="l" sx={{ pt: 2 }}>
                    <Routes>
                        <Route path = "/advertisement" element={<Advertisement/>}/>
                        <Route path = "/controls" element={<Controls/>}/>
                    </Routes>
                </Container>
            </div>
        </BrowserRouter>
    );
}

export default App;
