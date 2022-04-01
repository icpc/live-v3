import React from "react";
import "./App.css";
import PresetsPanel from "./PresetsPanel";

class AdvertisementPanel extends PresetsPanel {
}

AdvertisementPanel.defaultProps = {
    ...AdvertisementPanel.defaultProps,
    path: "/advertisement",
    tableKeys: ["text"],
};

function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementPanel/>
        </div>
    );
}

export default Advertisement;
