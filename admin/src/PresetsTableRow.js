import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import { grey } from "@mui/material/colors";

import { ShowPresetButton, onClickShow } from "./ShowPresetButton";
import { BACKEND_API_URL } from "./config";

const onClickEdit = (currentRow) => () => {
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({ ...state, editValue: state.value }));
    } else {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(currentRow.state.editValue.content),
        };
        fetch(BACKEND_API_URL + currentRow.props.path + "/" + currentRow.props.row.id, requestOptions)
            .then(response => response.json())
            .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
            .then(currentRow.props.updateTable)
            .catch(currentRow.props.onErrorHandle("Failed to edit preset"));
    }
};

const onClickDelete = (currentRow) => () => {
    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    };
    fetch(BACKEND_API_URL + currentRow.props.path + "/" + currentRow.props.row.id + "/delete", requestOptions)
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
            active: !(props.row.widgetId === null)
        };
    }

    render() {
        const currentRow = this;
        return (<TableRow
            key={this.state.value["id"]}
            sx={{ backgroundColor: (this.state.active ? this.props.activeColor : this.props.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowPresetButton
                    onClick={onClickShow(currentRow)}
                    active={this.state.active}
                />
            </TableCell>
            {this.props.tableKeys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    {this.state.editValue === undefined ? this.state.value.content[rowKey] : (
                        <Box onSubmit={onClickEdit(currentRow)} component="form" type="submit">
                            <TextField
                                hiddenLabel
                                defaultValue={this.state.value.content[rowKey]}
                                id="filled-hidden-label-small"
                                type="text"
                                size="small"
                                sx={{ width: 1 }}
                                onChange={(e) => {
                                    currentRow.state.editValue.content[rowKey] = e.target.value;
                                }}
                            />
                        </Box>)
                    }
                </TableCell>
            ))}
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color="inherit" onClick={onClickEdit(currentRow)}>
                        {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={onClickDelete(currentRow)}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>
        </TableRow>);
    }
}
