import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import { grey } from "@mui/material/colors";

import { ShowPresetButton, onClickShow } from "./ShowPresetButton";
import { BASE_URL_BACKEND } from "../config";

const getSettings = (row) => {
    return row.settings;
};

const onClickEdit = (currentRow) => () => {
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({ ...state, editValue: state.value }));
    } else {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(getSettings(currentRow.state.editValue)),
        };
        fetch(BASE_URL_BACKEND + currentRow.props.path + "/" + currentRow.props.row.id, requestOptions)
            .then(response => response.json())
            .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
            .then(currentRow.props.updateTable)
            .catch(currentRow.props.onErrorHandle("Failed to edit preset"));
    }
};

const onClickDelete = (currentRow) => () => {
    const requestOptions = {
        method: "DELETE",
        headers: { "Content-Type": "application/json" }
    };
    fetch(BASE_URL_BACKEND + currentRow.props.path + "/" + currentRow.props.row.id, requestOptions)
        .then(response => response.json())
        .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
        .then(currentRow.props.updateTable)
        .catch(currentRow.props.onErrorHandle("Failed to delete preset"));
};

export class PresetsTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.row,
            editValue: undefined,
            shown: props.row.shown
        };
    }

    render() {
        const currentRow = this;
        return (<TableRow
            key={this.state.value["id"]}
            sx={{ backgroundColor: (this.state.shown ? this.props.activeColor : this.props.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowPresetButton
                    onClick={onClickShow(currentRow)}
                    active={this.state.shown}
                />
            </TableCell>
            {this.props.tableKeys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    {this.state.editValue === undefined ? getSettings(this.state.value)[rowKey] : (
                        <Box onSubmit={onClickEdit(currentRow)} component="form" type="submit">
                            <TextField
                                autoFocus
                                hiddenLabel
                                defaultValue={getSettings(this.state.value)[rowKey]}
                                id="filled-hidden-label-small"
                                type="text"
                                size="small"
                                sx={{ width: 1 }}
                                onChange={(e) => {
                                    getSettings(currentRow.state.editValue)[rowKey] = e.target.value;
                                }}
                            />
                        </Box>)
                    }
                </TableCell>
            ))}
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"} onClick={onClickEdit(currentRow)}>
                        {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={onClickDelete(currentRow)}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>
        </TableRow>);
    }
}
