import React, { FormEvent } from "react";
import { TableCell, TableRow } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ShowPresetButton from "./controls/ShowPresetButton.tsx";
import { PresetsTableCell } from "./PresetsTableCell.tsx";
import { activeRowColor } from "../styles.js";
import {
    usePresetTableRowDataState,
    type PresetData,
} from "./PresetsTableRowUtils.tsx";

interface PresetsTableRowProps {
    data: PresetData;
    tableKeys: string[];
    onShow: () => void;
    onEdit: (data: PresetData) => unknown;
    onDelete: () => void;
    isImmutable?: boolean;
}

function getRowBackgroundColor(isShown: boolean): string | undefined {
    return isShown ? activeRowColor : undefined;
}

function getEditButtonIcon(isEditMode: boolean): React.ReactElement {
    return isEditMode ? <SaveIcon /> : <EditIcon />;
}

function getEditButtonColor(isEditMode: boolean): "inherit" | "primary" {
    return isEditMode ? "primary" : "inherit";
}

function ShowButtonCell({
    onShow,
    checked,
}: {
    onShow: () => void;
    checked: boolean;
}): React.ReactElement {
    return (
        <TableCell component="th" scope="row" align="left">
            <ShowPresetButton onClick={onShow} checked={checked} />
        </TableCell>
    );
}

function ManagementButtonsCell({
    isEditMode,
    onClickEdit,
    onDelete,
}: {
    isEditMode: boolean;
    onClickEdit: () => void;
    onDelete: () => void;
}): React.ReactElement {
    return (
        <TableCell component="th" scope="row" align="right">
            <Box>
                <IconButton
                    color={getEditButtonColor(isEditMode)}
                    onClick={onClickEdit}
                    aria-label={isEditMode ? "Save changes" : "Edit row"}
                >
                    {getEditButtonIcon(isEditMode)}
                </IconButton>
                <IconButton
                    color="error"
                    onClick={onDelete}
                    aria-label="Delete row"
                >
                    <DeleteIcon />
                </IconButton>
            </Box>
        </TableCell>
    );
}

export function PresetsTableRow({
    data,
    tableKeys,
    onShow,
    onEdit,
    onDelete,
    isImmutable = false,
}: PresetsTableRowProps): React.ReactElement {
    const { editData, onClickEdit, onSubmitEdit, onChangeField } =
        usePresetTableRowDataState(data, onEdit);

    function isEditMode(): boolean {
        return editData !== undefined;
    }

    function DataCell({
        rowKey,
        data,
        editData,
        onChangeField,
        onSubmitEdit,
    }: {
        key?: string;
        rowKey: string;
        data: PresetData;
        editData: PresetData | undefined;
        onChangeField: (rowKey: string) => (value: unknown) => void;
        onSubmitEdit: (event: FormEvent<HTMLFormElement>) => void;
    }): React.ReactElement {
        return (
            <PresetsTableCell
                value={data.settings[rowKey]}
                editValue={editData?.settings[rowKey]}
                onChange={onChangeField(rowKey)}
                onSubmit={onSubmitEdit}
            />
        );
    }

    return (
        <TableRow
            key={data.id}
            sx={{ backgroundColor: getRowBackgroundColor(data.shown) }}
        >
            <ShowButtonCell onShow={onShow} checked={data.shown} />
            {tableKeys.map((rowKey) => (
                <DataCell
                    key={rowKey}
                    rowKey={rowKey}
                    data={data}
                    editData={editData}
                    onChangeField={onChangeField}
                    onSubmitEdit={onSubmitEdit}
                />
            ))}
            {!isImmutable && (
                <ManagementButtonsCell
                    isEditMode={isEditMode()}
                    onClickEdit={onClickEdit}
                    onDelete={onDelete}
                />
            )}
        </TableRow>
    );
}

export type { PresetsTableRowProps };
