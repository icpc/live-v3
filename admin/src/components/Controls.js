import React from "react";

import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND } from "../config";

const controlElements = [
    { text: "Scoreboard", path: "/scoreboard" },
    { text: "Queue", path: "/queue" },
    { text: "Statistics", path: "/statistics" },
    { text: "Ticker", path: "/ticker" }];

class ControlsTable extends PresetsTable {
    updateData() {
        Promise.all(
            controlElements.map(element =>
                fetch(BASE_URL_BACKEND + element.path)
                    .then(r => r.json())
                    .then(r => ({ id: element.path, settings: { text: element.text }, shown: r.shown }))
            ))
            .then(elements => this.setState(state => ({
                ...state,
                dataElements: elements,
            })));
    }
}

ControlsTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "",
    apiTableKeys: ["text"],
    isImmutable: true,
};

function Controls() {
    const { enqueueSnackbar, } = useSnackbar();
    return (
        <div className="Controls">
            <ControlsTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </div>
    );
}

export default Controls;
