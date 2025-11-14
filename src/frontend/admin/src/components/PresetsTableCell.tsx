import { TableCell, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import React, { FormEvent, ChangeEvent } from "react";

interface ValueEditorProps<T = unknown> {
    value: T;
    onChange: (value: T) => void;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

interface PresetsTableCellProps<T = unknown> {
    value: T;
    onChange: (value: T) => void;
    editValue?: T;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    valuePrinter?: (value: T) => React.ReactNode;
    ValueEditor?: React.ComponentType<ValueEditorProps<T>>;
}

function defaultValuePrinter<T>(value: T): React.ReactNode {
    return String(value);
}

function defaultValueEditor<T>({
    onSubmit,
    value,
    onChange,
}: ValueEditorProps<T>): React.ReactElement {
    function handleChange(event: ChangeEvent<HTMLInputElement>): void {
        onChange(event.target.value as T);
    }

    return (
        <Box
            component="form"
            onSubmit={onSubmit}
            sx={{ display: "inline-flex", width: "100%" }}
        >
            <TextField
                autoFocus
                hiddenLabel
                defaultValue={String(value)}
                id="filled-hidden-label-small"
                type="text"
                size="small"
                sx={{ width: 1 }}
                onChange={handleChange}
            />
        </Box>
    );
}

export function PresetsTableCell<T = unknown>({
    value,
    onChange,
    editValue,
    onSubmit,
    valuePrinter = defaultValuePrinter,
    ValueEditor = defaultValueEditor,
}: PresetsTableCellProps<T>): React.ReactElement {
    function isEditMode(): boolean {
        return editValue !== undefined;
    }

    function renderCellComponent(): React.ReactNode {
        if (isEditMode()) {
            return (
                <ValueEditor
                    onSubmit={onSubmit}
                    value={value}
                    onChange={onChange}
                />
            );
        }
        return valuePrinter(value);
    }

    return (
        <TableCell component="th" scope="row">
            {renderCellComponent()}
        </TableCell>
    );
}

export type { ValueEditorProps };
