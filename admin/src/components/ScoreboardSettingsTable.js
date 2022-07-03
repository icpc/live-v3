import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import {
    ButtonGroup,
    Switch,
    Table,
    TableHead,
    TableBody,
    TableCell,
    TableRow, TextField,
    Button,
    Container,
    Checkbox,
    Typography
} from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { BASE_URL_BACKEND } from "../config";

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

function NumericField(props) {
    return (<div>
        <TextField type="number" size="small" onChange={(e) => {
            props.onChange(e.target.value);
        }} value={props.value} sx={{ maxWidth: 0.5 }}/>
    </div>);
}

NumericField.propTypes = {
    value: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
};

function ScoreboardSettings() {
    const { enqueueSnackbar, } = useSnackbar();
    const createErrorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const defaultRegions = ["all"];

    const [sRegions, setSRegions] = useState(
        defaultRegions
    );
    const [sShown, setSShown] = useState(false);
    const [sSettings, setSSettings] = useState({
        isInfinite: true,
        optimismLevel: "Normal",
        region: "all",
        startFromPage: 1,
        numPages: 100,
    });

    const update = () => {
        fetch(apiUrl)
            .then(res => res.json())
            .then(
                (result) => {
                    // testing code, delete it please
                    setSShown(result.shown);
                    if (result.settings.region === undefined) {
                        result.settings.region = "all";
                    }
                    if (result.settings.numPages === undefined) {
                        result.settings.numPages = 100;
                    }
                    // testing code, delete it please
                    setSSettings(result.settings);
                })
            .catch(createErrorHandler("Failed to load list of presets"));
        fetch(apiUrl + "/info")
            .then(res => res.json())
            .then(
                (result) => {
                    setSRegions([...defaultRegions, ...result]);
                })
            .catch(createErrorHandler("Failed to load info"));
    };

    useEffect(() => {
        update();
    }, []);

    const onClickHide = () => {
        apiPost("/hide")
            .then(update)
            .catch(createErrorHandler("Failed to hide scoreboard"));
    };

    const onClickShow = () => {
        apiPost("/show_with_settings", sSettings)
            .then(() => setSShown(true))
            .catch(createErrorHandler("Failed to show scoreboard"));
    };

    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="ScoreboardSettings">
            <Table sx={{ m: 2 }}>
                <TableHead>
                    <TableRow>
                        <TableCell>
                            <Typography variant="h6">Region</Typography>
                        </TableCell>
                        {["Normal", "Optimistic", "Pessimistic"].map(val =>
                            <TableCell key={val} align="right">
                                <Typography variant="h6">{val}</Typography>
                            </TableCell>
                        )}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {sRegions.map(region =>
                        <TableRow key={region}>
                            <TableCell>
                                {region}
                            </TableCell>
                            {["normal", "optimistic", "pessimistic"].map(type =>
                                <TableCell key={type} align="right">
                                    <Checkbox
                                        checked={sSettings.region === region && sSettings.optimismLevel === type}
                                        sx={{ "& .MuiSvgIcon-root": { fontSize: 24 } }}
                                        onChange={() => setSSettings(state => ({
                                            ...state,
                                            optimismLevel: type,
                                            region: region
                                        }))}
                                    />
                                </TableCell>
                            )}
                        </TableRow>
                    )}
                </TableBody>
            </Table>
            <Table align="center" sx={{ m: 2 }}>
                <TableHead>
                    <TableRow>
                        {["Start from", "Amount", "Infinity"].map(val =>
                            <TableCell key={val}>
                                <Typography variant="h6">{val}</Typography>
                            </TableCell>
                        )}
                    </TableRow>
                </TableHead>
                <TableBody>
                    <TableRow>
                        <TableCell>
                            <NumericField value={sSettings.startFromPage} onChange={(v) => {
                                setSSettings(state => ({ ...state, startFromPage: v }));
                            }}/>
                        </TableCell>
                        <TableCell>
                            <NumericField value={sSettings.numPages} onChange={(v) => {
                                setSSettings(state => ({ ...state, numPages: v }));
                            }}/>
                        </TableCell>
                        <TableCell><Switch checked={sSettings.isInfinite}
                            onChange={t => setSSettings(state => ({
                                ...state,
                                isInfinite: t.target.checked
                            }))}/>
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
            <ButtonGroup variant="contained" sx={{ m: 2 }}>
                <Button color="primary" disabled={sShown === true} onClick={onClickShow}>Show</Button>
                <Button color="error" disabled={sShown !== true} onClick={onClickHide}>Hide</Button>
            </ButtonGroup>
        </Container>
    );
}

export default ScoreboardSettings;
