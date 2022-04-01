import React from "react";
import "./App.css";
import PresetsPanel from "./PresetsPanel";

class PicturePanel extends PresetsPanel {
}

PicturePanel.defaultProps = {
    path: "/picture",
    tableKeys: ["name", "url"],
};

function Picture() {
    return (
        <div className="Pictures">
            <PicturePanel/>
        </div>
    );
}

export default Picture;
