import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import {
    ButtonGroup,
    Box, IconButton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableRow, TextField,
    ToggleButton,
    ToggleButtonGroup,
    Button,
    Container
} from "@mui/material";
import ArrowBackIosIcon from "@mui/icons-material/ArrowBackIos";
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos";
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
        <IconButton onClick={() => props.onChange(props.value - 1)}><ArrowBackIosIcon/></IconButton>
        <TextField type="number" size="small" onChange={(e) => {
            props.onChange(e.target.value);
        }} value={props.value} sx={{ maxWidth: 0.5 }}/>
        <IconButton onClick={() => props.onChange(props.value + 1)}><ArrowForwardIosIcon/></IconButton>
    </div>);
}

NumericField.propTypes = {
    value: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
};

function ScoreboardSettings() {
    const { enqueueSnackbar, } = useSnackbar();
    const createErrorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const [sShown, setSShown] = useState(false);
    const [sSettings, setSSettings] = useState({
        isInfinite: true,
        optimismLevel: "Normal",
        startFromPage: 1,
        numPages: undefined,
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
            <Table align={"center"}>
                <TableBody>
                    <TableRow>
                        <TableCell>Visibility</TableCell>
                        <TableCell align="right">
                            <ButtonGroup variant="contained" aria-label="outlined primary button group">
                                <Button color="primary" disabled={sShown === true} onClick={onClickShow}>Show</Button>
                                <Button color="error" disabled={sShown !== true} onClick={onClickHide}>Hide</Button>
                            </ButtonGroup>
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Infinity playing</TableCell>
                        <TableCell align="right"><Switch checked={sSettings.isInfinite}
                            onChange={t => setSSettings(state => ({
                                ...state,
                                isInfinite: t.target.checked
                            }))}/>
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Optimistic level</TableCell>
                        <TableCell align="right"><ToggleButtonGroup color="primary" value={sSettings.optimismLevel}
                            exclusive onChange={(e) => {
                                setSSettings(state => ({ ...state, optimismLevel: e.target.value }));
                            }}>
                            {["normal", "optimistic", "pessimistic"].map(type =>
                                <ToggleButton value={type} key={type}>{type.substr(0, 6)}</ToggleButton>)}
                        </ToggleButtonGroup></TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Start from page</TableCell>
                        <TableCell align="right">
                            <NumericField value={sSettings.startFromPage} onChange={(v) => {
                                setSSettings(state => ({ ...state, startFromPage: v }));
                            }}/>
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>Num pages</TableCell>
                        <TableCell align="right">
                            <Box sx={{ display: "flex", justifyContent: "flex-end", flexDirection: "row" }}>
                                <Switch checked={sSettings.numPages !== undefined}
                                    onChange={t => setSSettings(state => ({
                                        ...state,
                                        numPages: t.target.checked ? 1 : undefined
                                    }))}/>
                                {sSettings.numPages !== undefined &&
                                <NumericField value={sSettings.numPages} onChange={(v) => {
                                    setSSettings(state => ({ ...state, numPages: v }));
                                }}/>}
                            </Box>
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        </Container>
    );
}

export default ScoreboardSettings;
