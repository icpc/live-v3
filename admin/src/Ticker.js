import React from "react";

import "./App.css";
import { lightBlue } from "@mui/material/colors";

import PresetsPanel from "./PresetsPanel";

class TickerPanel extends PresetsPanel {
    constructor(props) {
        super(props);
    }
    render() {
        return <PresetsPanel path={ this.props.path } activeColor={ lightBlue[100] } inactiveColor={ "white" } />;
    }
}

function Ticker() {
    return (
        <div className="TickerPanel">
            <TickerPanel path="/ticker"/>
        </div>
    );
}

export default Ticker;
