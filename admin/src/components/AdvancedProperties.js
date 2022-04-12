import React, { useState } from "react";
import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { PropsTableRow } from "./PropsTableRow";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { Button } from "@mui/material";
import { BASE_URL_BACKEND } from "../config";

export class PropsTable extends PresetsTable {
    constructor(props) {
        super(props);
    }
}

const apiUrl = () => {
    return BASE_URL_BACKEND + "/advancedProperties";
};

const apiPost = (path, body = {}, method = "POST") => {
    const requestOptions = {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    };
    return fetch(apiUrl() + path, requestOptions)
        .then(response => response.json())
        .then(response => {
            if (response.status !== "ok") {
                throw new Error("Server return not ok status: " + response);
            }
            return response;
        });
};

PropsTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/advancedProperties",
    apiTableKeys: ["key", "value"],
    rowComponent: PropsTableRow,
};

function AdvancedSettings() {
    const { enqueueSnackbar,  } = useSnackbar();
    const [loaded, setLoaded] = useState(true);

    return (
        (loaded && <div className="AdvancedProperties">
            <PropsTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
            <Button
                onClick={async () => {
                    setLoaded(false);
                    await apiPost("/reload");
                    setLoaded(true);
                }}>
                Reload
            </Button>
        </div>)
    );
}

export default AdvancedSettings;
