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

const getSettings = (row) => {
    return row;
};

export class PropsTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.rowData,
            editValue: undefined,
        };
        this.onClickEdit = this.onClickEdit.bind(this);
        this.onSubmitEdit = this.onSubmitEdit.bind(this);
        this.onClickDelete = this.onClickDelete.bind(this);
        this.onClickShow = this.onClickShow.bind(this);
    }

    onClickEdit() {
        if (this.state.editValue === undefined) {
            this.setState(state => ({ ...state, editValue: state.value }));
        } else {
            this.props.apiPostFunc("/" + this.props.rowData.id, getSettings(this.state.editValue))
                .then(() => this.setState(state => ({ ...state, editValue: undefined })))
                .then(this.props.updateTable)
                .catch(this.props.createErrorHandler("Failed to edit preset"));
        }
    }

    onSubmitEdit(e) {
        e.preventDefault();
        this.onClickEdit();
    }

    onClickDelete() {
        this.props.apiPostFunc("/" + this.props.rowData.id, {}, "DELETE")
            .then(this.props.updateTable)
            .catch(this.props.createErrorHandler("Failed to delete preset"));
    }

    onClickShow() {
        this.props.apiPostFunc("/" + this.props.rowData.id + (this.props.rowData.shown ? "/hide" : "/show"))
            .then(this.props.updateTable)
            .catch(this.props.createErrorHandler("Failed to show or hide preset"));
    }

    render() {
        return (<TableRow
            key={this.state.value.key}>
            {/* <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
                <ShowPresetButton
                    onClick={this.onClickShow}
                    active={this.props.rowData.shown}
                />
            </TableCell> */}
            {this.props.apiTableKeys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    {this.state.editValue === undefined ? getSettings(this.state.value)[rowKey] : (
                        <Box onSubmit={this.onSubmitEdit} component="form" type="submit">
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
            {/* {this.props.isImmutable !== true &&
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                        onClick={this.onClickEdit}>
                        {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={this.onClickDelete}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>} */}
        </TableRow>);
    }
}

PropsTableRow.propTypes = {
    apiPostFunc: PropTypes.func.isRequired,
    apiTableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateTable: PropTypes.func.isRequired,
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
    }).isRequired,
    rowData: PropTypes.shape({
        key: PropTypes.string.isRequired,
        value: PropTypes.string.isRequired,
    }),
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};
