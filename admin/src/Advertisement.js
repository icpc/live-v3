import React from "react";

import "./App.css";

import { PresetsPanel } from "./PresetsPanel";

class AdvertisementPanel extends PresetsPanel {
}

function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementPanel/>
        </div>
    );
}

export default Advertisement;
