import { useState, FormEvent } from "react";

type PresetSettings = Record<string, unknown>;

interface PresetData {
    id: string | number;
    shown: boolean;
    settings: PresetSettings;
}

type SetEditDataAction = (updateFn: (prevData: PresetData) => PresetData) => void;

interface UsePresetTableRowDataStateReturn {
    editData: PresetData | undefined;
    onClickEdit: () => void;
    onSubmitEdit: (event: FormEvent<HTMLFormElement>) => void;
    onChangeField: (rowKey: string) => (value: unknown) => void;
}

function createUpdatedPresetData(
    currentData: PresetData,
    rowKey: string,
    newValue: unknown,
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
    rowKey: string,
    setEditData: SetEditDataAction
) {
    return (value: unknown) => {
        setEditData((prevData: PresetData) =>
            createUpdatedPresetData(prevData, rowKey, value)
        );
    };
}

function createFieldChangeEventHandler(
    rowKey: string,
    setEditData: SetEditDataAction
) {
    return (e: { target: { value: unknown } }) => {
        createFieldChangeHandler(rowKey, setEditData)(e.target.value);
    };
}

function usePresetTableRowDataState(
    data: PresetData,
    onEdit: (data: PresetData) => unknown
): UsePresetTableRowDataStateReturn {
    const [editData, setEditData] = useState<PresetData | undefined>(undefined);

    const onClickEdit = () => {
        if (editData) {
            onEdit(editData);
            setEditData(undefined);
        } else {
            setEditData(data);
        }
    };

    const onSubmitEdit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (editData) {
            onEdit(editData);
            setEditData(undefined);
        }
    };

    const onChangeField = (rowKey: string) =>
        createFieldChangeHandler(rowKey, setEditData);

    return {
        editData,
        onClickEdit,
        onSubmitEdit,
        onChangeField
    };
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
    UsePresetTableRowDataStateReturn,
};
