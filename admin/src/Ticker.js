import React from "react";

import "./App.css";
import { lightBlue } from "@mui/material/colors";

import PresetsPanel from "./PresetsPanel";

class TickerPanel extends PresetsPanel {
    constructor(props) {
        super(props);
    }
    render() {
        return <PresetsPanel activeColor={ lightBlue[100] } inactiveColor={ "white" } />;
    }
}

function Ticker() {
    return (
        <div className="TickerPanel">
            <TickerPanel/>
        </div>
    );
}

export default Ticker;
