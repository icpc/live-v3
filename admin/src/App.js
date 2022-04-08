import React from "react";
import "./App.css";
import AppNav from "./AppNav";
import Container from "@mui/material/Container";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import TickerMessage from "./components/TickerMessage";
import Controls from "./components/Controls";
import Advertisement from "./components/Advertisement";
import Picture from "./components/Picture";
import { SnackbarProvider } from "notistack";


function App() {
    return (
        <BrowserRouter>
            <SnackbarProvider maxSnack={5}>
                <div className="App">
                    <AppNav/>
                    <Container maxWidth="md" sx={{ pt: 2 }}>
                        <Routes>
                            <Route path="/" element={<Controls/>}/>
                            <Route path="/controls" element={<Controls/>}/>
                            <Route path="/advertisement" element={<Advertisement/>}/>
                            <Route path="/picture" element={<Picture/>}/>
                            <Route path="/ticker" element={<TickerMessage/>}/>
                        </Routes>
                    </Container>
                </div>
            </SnackbarProvider>
        </BrowserRouter>
    );
}

export default App;
