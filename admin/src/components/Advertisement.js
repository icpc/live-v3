import React from "react";
import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

class AdvertisementTable extends PresetsTable {
}

AdvertisementTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/advertisement",
    apiTableKeys: ["text"],
};

function Advertisement() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <div className="Advertisement">
            <AdvertisementTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </div>
    );
}

export default Advertisement;
