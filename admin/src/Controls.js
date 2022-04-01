import React from "react";

import "./App.css";
import { lightBlue } from "@mui/material/colors";

import ControlsPanel from "./ControlsPanel";

function Controls() {
    return (
        <div className="ControlsPanel">
            <ControlsPanel activeColor={lightBlue[100]} inactiveColor={"white"}/>
        </div>
    );
}

export default Controls;
