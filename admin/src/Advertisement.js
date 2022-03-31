import React from "react";

import "./App.css";
import { lightBlue } from "@mui/material/colors";


import PresetsPanel from "./PresetsPanel";

class AdvertisementPanel extends PresetsPanel {
    constructor(props) {
        super(props);
    }
    render() {
        return <PresetsPanel path={ this.props.path } activeColor={ lightBlue[100] } inactiveColor={ "white" }/>;
    }
}

function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementPanel path="/advertisement"/>
        </div>
    );
}

export default Advertisement;
