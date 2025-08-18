import React, { useState, FormEvent, ChangeEvent } from "react";
import { TableCell, TableRow } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ShowPresetButton from "./controls/ShowPresetButton.tsx";
import { PresetsTableCell } from "./PresetsTableCell.tsx";
import { activeRowColor } from "../styles.js";

type PresetSettings = Record<string, unknown>;

interface PresetData {
    id: string | number;
    shown: boolean;
    settings: PresetSettings;
}

interface PresetsTableRowProps {
    data: PresetData;
    tableKeys: string[];
    onShow: () => void;
    onEdit: (data: PresetData) => unknown;
    onDelete: () => void;
    isImmutable?: boolean;
}

type SetEditDataAction = (updateFn: (prevData: PresetData) => PresetData) => void;

interface UsePresetTableRowDataStateReturn {
    editData: PresetData | undefined;
    onClickEdit: () => void;
    onSubmitEdit: (event: FormEvent<HTMLFormElement>) => void;
    onChangeField: (rowKey: string) => (value: any) => void;
}

function createUpdatedPresetData(
    currentData: PresetData,
    rowKey: string,
    newValue: any,
): PresetData {
    return {
        ...currentData,
        settings: {
            ...currentData.settings,
            [rowKey]: newValue
        }
    };
}

function createFieldChangeHandler(
    setEditData: SetEditDataAction,
    rowKey: string,
): (value: any) => void {
    return function handleFieldChange(value: any): void {
        setEditData((prevData) => createUpdatedPresetData(prevData, rowKey, value));
    };
}

function createFieldChangeEventHandler(
    setEditData: SetEditDataAction,
    rowKey: string,
): (event: ChangeEvent<HTMLInputElement>) => void {
    return function handleFieldChangeEvent(event: ChangeEvent<HTMLInputElement>): void {
        const value = event.target.value;
        setEditData((prevData) => createUpdatedPresetData(prevData, rowKey, value));
    };
}

function usePresetTableRowDataState(
    data: PresetData,
    onEdit: (data: PresetData) => unknown,
): UsePresetTableRowDataStateReturn {
    const [editData, setEditData] = useState<PresetData | undefined>(undefined);

    function isEditMode(): boolean {
        return editData !== undefined;
    }

    function startEdit(): void {
        setEditData(data);
    }

    function cancelEdit(): void {
        setEditData(undefined);
    }

    async function saveEdit(): Promise<void> {
        if (editData) {
            await onEdit(editData);
            cancelEdit();
        }
    }

    function handleClickEdit(): void {
        if (isEditMode()) {
          saveEdit();
        } else {
          startEdit();
        }
    }

    function handleSubmitEdit(event: FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        handleClickEdit();
    }

    function createChangeFieldHandler(rowKey: string): (value: any) => void {
        return createFieldChangeHandler(setEditData as SetEditDataAction, rowKey);
    }

    return {
        editData,
        onClickEdit: handleClickEdit,
        onSubmitEdit: handleSubmitEdit,
        onChangeField: createChangeFieldHandler,
    };
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

export function PresetsTableRow({
    data,
    tableKeys,
    onShow,
    onEdit,
    onDelete,
    isImmutable = false,
} : PresetsTableRowProps): React.ReactElement {
    const {
        editData,
        onClickEdit,
        onSubmitEdit,
        onChangeField
    } = usePresetTableRowDataState(data, onEdit);

    function isEditMode(): boolean {
        return editData !== undefined;
    }

    function ShowButtonCell({
        onShow,
        checked
    }: {
        onShow: () => void;
        checked: boolean;
    }): React.ReactElement {
        return (
            <TableCell component="th" scope="row" align="left">
                <ShowPresetButton
                    onClick={onShow}
                    checked={checked}
                />
            </TableCell>
        );
    }

    function DataCell({
        rowKey,
        data,
        editData,
        onChangeField,
        onSubmitEdit
    }: {
        rowKey: string;
        data: PresetData;
        editData: PresetData | undefined;
        onChangeField: (rowKey: string) => (value: any) => void;
        onSubmitEdit: (event: FormEvent<HTMLFormElement>) => void;
    }): React.ReactElement {
        return (
            <PresetsTableCell
                key={rowKey}
                value={data.settings[rowKey]}
                editValue={editData?.settings[rowKey]}
                onChange={onChangeField(rowKey)}
                onSubmit={onSubmitEdit}
            />
        );
    }


    function ManagementButtonsCell({
        isEditMode,
        onClickEdit,
        onDelete
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

export {
    createFieldChangeHandler,
    createFieldChangeEventHandler,
    usePresetTableRowDataState,
    createUpdatedPresetData,
};

export type {
    PresetData,
    PresetSettings,
    PresetsTableRowProps,
    UsePresetTableRowDataStateReturn,
};
