import React, { useState } from "react";
import PropTypes from "prop-types";
import { TableCell, TableRow } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ShowPresetButton from "./ShowPresetButton";
import { PresetsTableCell } from "./PresetsTableCell";
import { activeRowColor } from "../styles";

export const onChangeFieldHandler = (stateChanger, rowKey) => ((v) => stateChanger((editData) => {
    editData.settings[rowKey] = v;
    return editData;
}));

export const onChangeFieldEventHandler = (stateChanger, rowKey) => ((e) => stateChanger((editData) => {
    editData.settings[rowKey] = e.target.value;
    return editData;
}));

export const usePresetTableRowDataState = (data, onEdit) => {
    const [editData, setEditData] = useState();

    const onClickEdit = () => {
        if (editData === undefined) {
            setEditData(data);
        } else {
            onEdit(editData).then(() => setEditData(undefined));
        }
    };
    const onSubmitEdit = (e) => {
        e.preventDefault();
        onClickEdit();
    };
    const onChangeField = rowKey => onChangeFieldHandler(setEditData, rowKey);
    return [editData, onClickEdit, onSubmitEdit, onChangeField];
};

export function PresetsTableRow({ data, tableKeys, onShow, onEdit, onDelete, isImmutable = false }) {
    const [editData, onClickEdit, onSubmitEdit, onChangeField] = usePresetTableRowDataState(data, onEdit);

    return (<TableRow
        key={data.id}
        sx={{ backgroundColor: (data.shown ? activeRowColor : undefined) }}>
        <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
            <ShowPresetButton
                onClick={onShow}
                active={data.shown}
            />
        </TableCell>
        {tableKeys.map((rowKey) => (
            <PresetsTableCell isActive={data.shown} key={rowKey} rowKey={rowKey}
                onSubmit={onSubmitEdit}
                value={data.settings[rowKey]}
                editValue={editData?.settings[rowKey]}
                onChange={onChangeField(rowKey)}/>
        ))}
        {isImmutable !== true &&
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color={editData === undefined ? "inherit" : "primary"} onClick={onClickEdit}>
                        {editData === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={onDelete}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>}
    </TableRow>);
}
PresetsTableRow.propTypes = {
    data: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.object.isRequired,
    }),
    tableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    onShow: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    isImmutable: PropTypes.bool,
};
