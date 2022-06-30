import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND } from "../config";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" }];

class ControlsTable extends PresetsTable {
    updateData() {
        Promise.all(
            controlElements.map(element =>
                fetch(BASE_URL_BACKEND + "/" + element.id)
                    .then(r => r.json())
                    .then(r => ({ id: element.id, settings: { text: element.text }, shown: r.shown }))
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
        <Container maxWidth="md" sx={{ pt: 2 }} className="Controls">
            <ControlsTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Controls;
