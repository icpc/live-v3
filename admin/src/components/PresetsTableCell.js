import { TableCell, TextField } from "@mui/material";
import { grey } from "@mui/material/colors";
import Box from "@mui/material/Box";
import React from "react";
import PropTypes from "prop-types";

const defaultValuePrinter = (v) => v;
const defaultValueEditor = ({ onSubmitAction, value, onChangeHandler }) => (
    <Box onSubmit={onSubmitAction} component="form" type="submit">
        <TextField
            autoFocus
            hiddenLabel
            defaultValue={value}
            id="filled-hidden-label-small"
            type="text"
            size="small"
            sx={{ width: 1 }}
            onChange={(e) => {
                onChangeHandler(e.target.value);
            }}
        />
    </Box>);

export const PresetsTableCell = ({
    isActive,
    value,
    onChangeValue,
    editValue,
    onSubmitAction,
    ValuePrinter = defaultValuePrinter,
    ValueEditor = defaultValueEditor
}) => {
    return (<TableCell
        component="th"
        scope="row"
        sx={{ color: (isActive ? grey[900] : grey[700]) }}>
        {editValue === undefined ? ValuePrinter(value) : <ValueEditor onSubmitAction={onSubmitAction} value={value} onChangeHandler={onChangeValue}/>}
    </TableCell>);
};

PresetsTableCell.propTypes = {
    isActive: PropTypes.bool.isRequired,
    value: PropTypes.any.isRequired,
    onChangeValue: PropTypes.func.isRequired,
    editValue: PropTypes.any,
    onSubmitAction: PropTypes.func.isRequired,
    ValuePrinter: PropTypes.elementType,
    ValueEditor: PropTypes.elementType,
};
