import React from "react";

import "../App.css";
import { lightBlue } from "@mui/material/colors";

import PresetsPanel from "./PresetsPanel";

class TickerPanel extends PresetsPanel {
}

TickerPanel.defaultProps = {
    ...TickerPanel.defaultProps,
    path: "/tickermessage",
    tableKeys: ["type", "text", "periodMs", "from", "to"],
};

function TickerMessage() {
    return (
        <div className="TickerPanel">
            <TickerPanel path="/tickermessage"/>
        </div>
    );
}

export default TickerMessage;

