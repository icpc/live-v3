import React from "react";
import "../App.css";
import { PresetsTable } from "./PresetsTable";

class AdvertisementTable extends PresetsTable {
}

AdvertisementTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/advertisement",
    apiTableKeys: ["text"],
};

function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementTable/>
        </div>
    );
}

export default Advertisement;
