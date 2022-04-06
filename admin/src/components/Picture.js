import React from "react";
import "../App.css";
import { PictureTableRow } from "./PictureTableRow";
import { PresetsTable } from "./PresetsTable";


export class PictureTable extends PresetsTable {
    constructor(props) {
        super(props);
    }
}

PictureTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/picture",
    apiTableKeys: ["name", "url"],
    rowComponent: PictureTableRow,
};

function Picture() {
    return (
        <div className="Pictures">
            <PictureTable/>
        </div>
    );
}

export default Picture;
