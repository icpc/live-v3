import React from "react";
import './App.css';
import AppNav from "./AppNav";
import Container from "@mui/material/Container";
import {PresetsPanel} from "./PresetsPanel";


function App() {
    return (
        <div className="App">
            <AppNav/>
            <Container maxWidth="xl" sx={{pt: 4}}>
                <PresetsPanel/>
            </Container>
        </div>
    );
}

export default App;
