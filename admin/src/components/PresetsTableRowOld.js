import React from "react";
import PropTypes from "prop-types";
import { TableCell, TableRow } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ShowPresetButton from "./ShowPresetButton";
import { PresetsTableCell } from "./PresetsTableCell";

export const onChangeSettingCellValue = (obj, rowKey) => ((v) => obj.setState((state) => {
    state.editValue.settings[rowKey] = v;
    return state;
}));

const getSettings = (row) => {
    return row.settings;
};

export class PresetsTableRowOld extends React.Component {
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
                .then(() => this.setState(state => ({ ...state, value: state.editValue, editValue: undefined })))
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
            key={this.state.value.id}
            sx={{ backgroundColor: (this.props.rowData.shown ? this.props.tStyle.activeColor : this.props.tStyle.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
                <ShowPresetButton
                    onClick={this.onClickShow}
                    active={this.props.rowData.shown}
                />
            </TableCell>
            {this.props.apiTableKeys.map((rowKey) => (
                <PresetsTableCell isActive={this.props.rowData.shown} key={rowKey} rowKey={rowKey}
                    onSubmitAction={this.onSubmitEdit}
                    value={this.state.value.settings[rowKey]}
                    editValue={this.state.editValue?.settings[rowKey]}
                    onChangeValue={onChangeSettingCellValue(this, rowKey)}/>
            ))}
            {this.props.isImmutable !== true &&
                <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                    <Box>
                        <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                            onClick={this.onClickEdit}>
                            {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                        </IconButton>
                        <IconButton color="error" onClick={this.onClickDelete}><DeleteIcon/></IconButton>
                    </Box>
                </TableCell>}
        </TableRow>);
    }
}

PresetsTableRowOld.propTypes = {
    apiPostFunc: PropTypes.func.isRequired,
    apiTableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateTable: PropTypes.func.isRequired,
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
    }).isRequired,
    rowData: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.object.isRequired,
    }),
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};
