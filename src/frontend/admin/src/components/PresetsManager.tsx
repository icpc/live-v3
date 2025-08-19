import React, { useEffect, useState, useMemo, useCallback } from "react";
import { Table, TableBody, TableCell, TableHead, TableRow, Box } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import { PresetsTableRow } from "./PresetsTableRow";

export interface Preset<S = Record<string, unknown>> {
    id: number | string;
    shown: boolean;
    settings: S;
}

export interface PresetService<S = Record<string, unknown>> {
    loadPresets(): Promise<Preset<S>[]>;
    createPreset(rowData: S): Promise<void> | void;
    editPreset(id: Preset<S>["id"], settings: S): Promise<void> | void;
    deletePreset(id: Preset<S>["id"]): Promise<void> | void;
    hidePreset(id: Preset<S>["id"]): Promise<void> | void;
    showPreset(id: Preset<S>["id"]): Promise<void> | void;
    addReloadDataHandler(cb: () => void): void;
    deleteReloadDataHandler(cb: () => void): void;
}

export interface RowComponentProps<S = Record<string, unknown>> {
    data: Preset<S>;
    tableKeys?: string[];
    onEdit: (data: Preset<S>) => void;
    onDelete: () => void;
    onShow: () => void;
    isImmutable?: boolean;
}

export interface AddButtonsProps<S = Record<string, unknown>> {
    onCreate: (rowData?: S) => void;
}

export interface PresetsManagerProps<S = Record<string, unknown>> {
    service: PresetService<S>;
    RowComponent?: React.ComponentType<RowComponentProps<S>>;
    defaultRowData?: Partial<S>;
    tableKeys: string[];
    tableKeysHeaders?: string[];
    rowsFilter?: (row: Preset<S>) => boolean;
    AddButtons?: React.ComponentType<AddButtonsProps<S>>;
    isImmutable?: boolean;
}

export const DefaultAddPresetButton = <S extends Record<string, unknown> = Record<string, unknown>>({
    onCreate,
}: AddButtonsProps<S>) => {
    return (
        <IconButton
            color="primary"
            size="large"
            onClick={() => onCreate()}
            aria-label="Add new preset"
        >
            <AddIcon />
        </IconButton>
    );
};

const createDefaultRowData = <S extends Record<string, unknown>>(
    tableKeys: string[],
    providedDefault?: Partial<S>
): S => {
    const baseData = tableKeys.reduce((acc, key) => {
        acc[key] = "";
        return acc;
    }, {} as Record<string, unknown>);

    return { ...baseData, ...providedDefault } as S;
};

export const PresetsManager = <S extends Record<string, unknown> = Record<string, unknown>>({
    service,
    RowComponent,
    defaultRowData,
    tableKeys,
    tableKeysHeaders,
    rowsFilter = () => true,
    AddButtons,
    isImmutable = false,
}: PresetsManagerProps<S>): React.ReactElement => {
    const [elements, setElements] = useState<Preset<S>[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const ResolvedRowComponent = RowComponent || (PresetsTableRow as React.ComponentType<RowComponentProps<S>>);
    const ResolvedAddButtons = AddButtons || (DefaultAddPresetButton as React.ComponentType<AddButtonsProps<S>>);

    const defaultRowDataResolved = useMemo(
        () => createDefaultRowData(tableKeys, defaultRowData),
        [defaultRowData, tableKeys]
    );

    const loadData = useCallback(async () => {
        try {
            setIsLoading(true);
            setError(null);
            const list = await service.loadPresets();
            setElements(list ?? []);
        } catch (err) {
            console.error("Failed to load presets:", err);
            setError("Failed to load presets");
        } finally {
            setIsLoading(false);
        }
    }, [service]);

    const handleCreate = useCallback(
        async (rowData?: S) => {
            try {
                await service.createPreset(rowData ?? defaultRowDataResolved);
            } catch (err) {
                console.error("Failed to create preset:", err);
                setError("Failed to create preset");
            }
        },
        [service, defaultRowDataResolved]
    );

    const handleEdit = useCallback(
        async (data: Preset<S>) => {
            try {
                await service.editPreset(data.id, data.settings);
            } catch (err) {
                console.error("Failed to edit preset:", err);
                setError("Failed to edit preset");
            }
        },
        [service]
    );

    const handleDelete = useCallback(
        async (id: Preset<S>["id"]) => {
            try {
                await service.deletePreset(id);
            } catch (err) {
                console.error("Failed to delete preset:", err);
                setError("Failed to delete preset");
            }
        },
        [service]
    );

    const handleToggleShow = useCallback(
        async (row: Preset<S>) => {
            try {
                if (row.shown) {
                    await service.hidePreset(row.id);
                } else {
                    await service.showPreset(row.id);
                }
            } catch (err) {
                console.error("Failed to toggle preset visibility:", err);
                setError("Failed to toggle preset visibility");
            }
        },
        [service]
    );

    useEffect(() => {
        loadData();
    }, [loadData]);

    useEffect(() => {
        service.addReloadDataHandler(loadData);
        return () => service.deleteReloadDataHandler(loadData);
    }, [service, loadData]);

    const filteredElements = useMemo(
        () => elements.filter(rowsFilter),
        [elements, rowsFilter]
    );

    return (
        <Box>
            {error && (
                <Box
                    sx={{
                        color: 'error.main',
                        mb: 2,
                        p: 1,
                        bgcolor: 'error.light',
                        borderRadius: 1
                    }}
                >
                    {error}
                </Box>
            )}

            <Table>
                {tableKeysHeaders && (
                    <TableHead>
                        <TableRow>
                            <TableCell width="48px" />
                            {tableKeysHeaders.map((header, index) => (
                                <TableCell
                                    key={`${header}-${index}`}
                                    sx={{ fontWeight: "bold" }}
                                >
                                    {header}
                                </TableCell>
                            ))}
                            <TableCell width="120px" />
                        </TableRow>
                    </TableHead>
                )}
                <TableBody>
                    {isLoading ? (
                        <TableRow>
                            <TableCell
                                colSpan={(tableKeysHeaders?.length ?? tableKeys.length) + 2}
                                align="center"
                            >
                                Loading...
                            </TableCell>
                        </TableRow>
                    ) : filteredElements.length === 0 ? (
                        <TableRow>
                            <TableCell
                                colSpan={(tableKeysHeaders?.length ?? tableKeys.length) + 2}
                                align="center"
                                sx={{ color: 'text.secondary' }}
                            >
                                No presets found
                            </TableCell>
                        </TableRow>
                    ) : (
                        filteredElements.map((row) => (
                            <ResolvedRowComponent
                                key={row.id}
                                data={row}
                                tableKeys={tableKeys}
                                onEdit={handleEdit}
                                onDelete={() => handleDelete(row.id)}
                                onShow={() => handleToggleShow(row)}
                                isImmutable={isImmutable}
                            />
                        ))
                    )}
                </TableBody>
            </Table>

            {!isImmutable && (
                <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
                    <ResolvedAddButtons onCreate={handleCreate} />
                </Box>
            )}
        </Box>
    );
};
