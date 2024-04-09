import { TableCell, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import React from "react";
import PropTypes from "prop-types";

export const ValueEditorPropTypes = {
    value: PropTypes.any.isRequired,
    onChange: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
};

const defaultValuePrinter = (v) => v;
const defaultValueEditor = ({ onSubmit, value, onChange }) => (
    <Box onSubmit={onSubmit} component="form" type="submit">
        <TextField
            autoFocus
            hiddenLabel
            defaultValue={value}
            id="filled-hidden-label-small"
            type="text"
            size="small"
            sx={{ width: 1 }}
            onChange={(e) => onChange(e.target.value)}
        />
    </Box>);

export const PresetsTableCell = ({
    value,
    onChange,
    editValue,
    onSubmit,
    valuePrinter = defaultValuePrinter,
    valueEditor : ValueEditor = defaultValueEditor
}) => {
    return (<TableCell component="th" scope="row">
        {editValue === undefined ? valuePrinter(value) : <ValueEditor onSubmit={onSubmit} value={value} onChange={onChange}/>}
    </TableCell>);
};

PresetsTableCell.propTypes = {
    isActive: PropTypes.bool.isRequired,
    value: PropTypes.any.isRequired,
    onChange: PropTypes.func.isRequired,
    editValue: PropTypes.any,
    onSubmit: PropTypes.func.isRequired,
    valuePrinter: PropTypes.elementType,
    valueEditor: PropTypes.elementType,
};
