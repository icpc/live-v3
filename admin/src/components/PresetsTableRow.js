import React from "react";
import PropTypes from "prop-types";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import { grey } from "@mui/material/colors";
import ShowPresetButton from "./ShowPresetButton";
import { addErrorHandler } from "../errors";

const getSettings = (row) => {
    return row.settings;
};

export const onClickEdit = (currentRow) => () => {
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({ ...state, editValue: state.value }));
    } else {
        currentRow.props.apiPostFunc("/" + currentRow.props.rowData.id, getSettings(currentRow.state.editValue))
            .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
            .then(currentRow.props.updateTable)
            .catch(addErrorHandler("Failed to edit preset"));
    }
};

export const onClickDelete = (currentRow) => () => {
    currentRow.props.apiPostFunc("/" + currentRow.props.rowData.id, {}, "DELETE")
        .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
        .then(currentRow.props.updateTable)
        .catch(addErrorHandler("Failed to delete preset"));
};

export const onClickShow = (currentRow) => () => {
    currentRow.props.apiPostFunc("/" + currentRow.props.rowData.id + (currentRow.props.rowData.shown ? "/hide" : "/show"))
        .then(currentRow.props.updateTable)
        .catch(addErrorHandler("Failed to hide preset"));
};

export class PresetsTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.rowData,
            editValue: undefined,
        };
    }

    render() {
        return (<TableRow
            key={this.state.value.id}
            sx={{ backgroundColor: (this.props.rowData.shown ? this.props.tStyle.activeColor : this.props.tStyle.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
                <ShowPresetButton
                    onClick={onClickShow(this)}
                    active={this.props.rowData.shown}
                />
            </TableCell>
            {this.props.apiTableKeys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    {this.state.editValue === undefined ? getSettings(this.state.value)[rowKey] : (
                        <Box onSubmit={onClickEdit(this)} component="form" type="submit">
                            <TextField
                                autoFocus
                                hiddenLabel
                                defaultValue={getSettings(this.state.value)[rowKey]}
                                id="filled-hidden-label-small"
                                type="text"
                                size="small"
                                sx={{ width: 1 }}
                                onChange={(e) => {
                                    getSettings(this.state.editValue)[rowKey] = e.target.value;
                                }}
                            />
                        </Box>)
                    }
                </TableCell>
            ))}
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                        onClick={onClickEdit(this)}>
                        {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={onClickDelete(this)}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>
        </TableRow>);
    }
}

PresetsTableRow.propTypes = {
    apiPostFunc: PropTypes.func.isRequired,
    apiTableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateTable: PropTypes.func.isRequired,
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
    }).isRequired,
};
