import React from "react";
import "../App.css";
import { PictureTableRow } from "./PictureTableRow";
import { PresetsTable } from "./PresetsTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";


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
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <div className="Pictures">
            <PictureTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </div>
    );
}

export default Picture;
