import { useSnackbar } from "notistack";
// import { errorHandlerWithSnackbar } from "../errors";
import React, { useEffect, useState } from "react";
import * as PropTypes from "prop-types";
import Box from "@mui/material/Box";
import {
    ButtonGroup,
    Grid,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    ToggleButton,
    ToggleButtonGroup
} from "@mui/material";
import ShowPresetButton from "./ShowPresetButton";
import Button from "@mui/material/Button";
import { BASE_URL_BACKEND } from "../config";
import { errorHandlerWithSnackbar } from "../errors";

const apiUrl = BASE_URL_BACKEND + "/scoreboard";

const apiPost = (path, body = {}, method = "POST") => {
    const requestOptions = {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    };
    return fetch(apiUrl + path, requestOptions)
        .then(response => response.json())
        .then(response => {
            if (response.status !== "ok") {
                throw new Error("Server return not ok status: " + response);
            }
            return response;
        });
};

function ScoreboardSettings(props) {
    const { enqueueSnackbar,  } = useSnackbar();
    const createErrorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const [sShown, setSShown] = useState(false);
    const [sSettings, setSSettings] = useState({
        isInfinite: true,
        optimismLevel: "Normal",
    });

    const update = () => {
        fetch(apiUrl)
            .then(res => res.json())
            .then(
                (result) => {
                    setSShown(result.shown);
                    setSSettings(result.settings);
                })
            .catch(createErrorHandler("Failed to load list of presets"));
    };

    useEffect(() => {
        update();
    }, []);

    const onClickHide = () => {
        apiPost("/hide").then(update);
    };

    const onClickShow = () => {
        apiPost("/show", sSettings).then(update);
    };

    return (
        <div className="ScoreboardSettings">
            <Table align={"center"}>
                <TableBody>
                    <TableRow>
                        <TableCell>Visibility</TableCell>
                        <TableCell>
                            <ButtonGroup variant="contained" aria-label="outlined primary button group">
                                <Button color="primary" disabled={sShown === true} onClick={onClickShow}>Show</Button>
                                <Button color="error" disabled={sShown !== true} onClick={onClickHide}>Hide</Button>
                            </ButtonGroup>
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Infinity playing</TableCell>
                        <TableCell><Switch checked={sSettings.isInfinite}
                            onChange={t => setSSettings(state => ({ ...state, isInfinite: t.target.checked }))}/>
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Infinity playing</TableCell>
                        <TableCell><ToggleButtonGroup color="primary" value={sSettings.optimismLevel}
                            exclusive onChange={(e) => {
                                setSSettings(state => ({ ...state, optimismLevel: e.target.value }));
                            }}>
                            {["normal", "optimistic", "pessimistic"].map(type =>
                                <ToggleButton value={type} key={type}>{type}</ToggleButton>)}
                        </ToggleButtonGroup></TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        </div>
    );
}

export default ScoreboardSettings;
