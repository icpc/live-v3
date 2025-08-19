// TODO: THINK about this component, i do not really like it
import React, { useEffect, useState, useMemo, useCallback } from "react";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import { PresetsTableRow } from "./PresetsTableRow";

type Preset<S extends Record<string, unknown>> = {
    id: number | string;
    shown: boolean;
    settings: S;
}

// TODO: think about types HERE
interface PresetService<S extends Record<string, unknown>> {
    loadPresets(): Promise<Preset<S>[]>;
    createPreset(rowData: S): unknown;
    editPreset(id: Preset<S>["id"], settings: S): unknown;
    deletePreset(id: Preset<S>["id"]): unknown;
    hidePreset(id: Preset<S>["id"]): unknown;
    showPreset(id: Preset<S>["id"]): unknown;
    addReloadDataHandler(cb: () => void): void;
    deleteReloadDataHandler(cb: () => void): void;
}

// TODO: THINK about types!
type RowComponentProps<S extends Record<string, unknown>> = {
    data: Preset<S>;
    tableKeys?: (keyof S & string)[];
    onEdit: (data: Preset<S>) => unknown;
    onDelete: () => void;
    onShow: () => void;
    isImmutable?: boolean;
};

type AddButtonsProps<S extends Record<string, unknown>> = {
    onCreate: (rowData?: S) => unknown;
};

export function DefaultAddPresetButton<S extends Record<string, unknown>>({
    onCreate,
}: AddButtonsProps<S>) {
    return (
        <IconButton color="primary" size="large" onClick={() => onCreate()}>
            <AddIcon />
        </IconButton>
    );
}

type PresetsManagerProps<S extends Record<string, unknown>> = {
    service: PresetService<S>;
    RowComponent?: React.ComponentType<RowComponentProps<S>>;
    defaultRowData?: S;
    tableKeys: (keyof S & string)[];
    tableKeysHeaders?: string[];
    rowsFilter?: (row: Preset<S>) => boolean;
    AddButtons?: React.ComponentType<AddButtonsProps<S>>;
    isImmutable?: boolean;
};


export function PresetsManager<S extends Record<string, unknown>>({
    service,
    RowComponent = PresetsTableRow as unknown as React.ComponentType<RowComponentProps<S>>,
    defaultRowData,
    tableKeys,
    tableKeysHeaders,
    rowsFilter = () => true,
    AddButtons = DefaultAddPresetButton as unknown as React.ComponentType<AddButtonsProps<S>>,
    isImmutable,
}: PresetsManagerProps<S>): React.ReactElement {
    const [elements, setElements] = useState<Preset<S>[]>([]);

    const defaultRowDataResolved = useMemo(() => {
        if (defaultRowData) return defaultRowData;
        return tableKeys.reduce((acc, key) => {
            (acc as Record<string, unknown>)[key] = "";
            return acc;
        }, {} as S);
    }, [defaultRowData, tableKeys]);

    const loadData = useCallback(async () => {
        const list = await service.loadPresets();
        setElements(list ?? []);
    }, [service]);

    const onCreate = useCallback(
        (rowData?: S) => service.createPreset(rowData ?? defaultRowDataResolved),
        [service, defaultRowDataResolved]
    );

    const onEdit = useCallback(
        (data: Preset<S>) => service.editPreset(data.id, data.settings),
        [service]
    );

    const onDelete = useCallback(
        (id: Preset<S>["id"]) => service.deletePreset(id),
        [service]
    );

    const onShow = useCallback(
        (row: Preset<S>) => (row.shown ? service.hidePreset(row.id) : service.showPreset(row.id)),
        [service]
    );

    useEffect(() => {loadData();}, [loadData]);
    useEffect(() => {
        service.addReloadDataHandler(loadData);
        return () => service.deleteReloadDataHandler(loadData);
    }, [service, loadData]);

    return (
        <div>
            <Table>
                {tableKeysHeaders && (
                    <TableHead>
                        <TableRow>
                            <TableCell key="__show_btn_row__" />
                            {tableKeysHeaders.map((hdr) => (
                                <TableCell key={hdr} sx={{ fontWeight: "bold" }}>
                                    {hdr}
                                </TableCell>
                            ))}
                            <TableCell />
                        </TableRow>
                    </TableHead>
                )}
                <TableBody>
                    {elements
                        ?.filter(rowsFilter)
                        .map((row) => (
                            <RowComponent
                                key={row.id}
                                data={row}
                                tableKeys={tableKeys}
                                onEdit={onEdit}
                                onDelete={() => onDelete(row.id)}
                                onShow={() => onShow(row)}
                                isImmutable={isImmutable}
                            />)
                        )
                    }
                </TableBody>
            </Table>
            {isImmutable !== true && <AddButtons onCreate={onCreate} />}
        </div>
    );
}

