import React, { ChangeEvent, useEffect, useState } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { Autocomplete, TextField, Button,
    TableCell, TableRow,
    Dialog, DialogTitle, DialogContent, DialogActions,
    CircularProgress, Card, Stack } from "@mui/material";
import ShowPresetButton from "./controls/ShowPresetButton.tsx";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import PreviewIcon from "@mui/icons-material/Preview";
import { PresetsTableCell, ValueEditorProps } from "./PresetsTableCell.tsx";
import { activeRowColor } from "../styles.js";
import { PresetSettings, usePresetTableRowDataState } from "./PresetsTableRow.tsx";
import { PresetsManager } from "./PresetsManager.jsx";
import { usePresetWidgetService } from "../services/presetWidget.js";
import { useTitleWidgetService } from "../services/titleWidget.js";
import { useDebounce } from "../utils.ts";

interface TitleSettings extends PresetSettings {
    preset: string;
    leftPreset: string;
    rightPreset: string;
    data: Record<string, unknown>;
}

interface TitleData {
    id: string | number;
    shown: boolean;
    settings: TitleSettings;
}

interface TitleTableRowProps {
    data: TitleData;
    onShow: () => void;
    onEdit: (data: TitleData) => unknown;
    onDelete: () => void;
}

interface PreviewSVGDialogProps {
    open: boolean;
    onClose: () => void;
    id: number;
}

interface ParamsLineProps {
    pKey: string;
    pValue: unknown;
}

function paramsDataToString(value: Record<string, unknown>): string {
    return Object.entries(value)
        .map(([key, value]) => `${key}: ${value}`)
        .join("\n");
}

function splitKeyValue(line: string): [string, string] | [] {
    const splitIndex = line.indexOf(":");
    if (splitIndex === -1) {
        return [];
    }

    const key = line.substring(0, splitIndex).trim();
    const value = line.substring(splitIndex + 1).trim();

    return key ? [key, value] : [];
}

function parseParamsData(input: string): Record<string, string> {
    if (!input.trim()) return {};

    return input
        .split("\n")
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .map(splitKeyValue)
        .filter((result): result is [string, string] => result.length === 2)
        .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});
}

function createPresetUpdater(
    data: TitleData,
    presetValue: string,
    onEdit: (data: TitleData) => unknown
): () => void {
    return function updatePreset() {
        const updatedData: TitleData = {
            ...data,
            settings: {
                ...data.settings,
                preset: presetValue,
            }
        };
        onEdit(updatedData);
    };
}


function PreviewSVGDialog({
    id,
    open,
    onClose
}: PresetsManager): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const service = useTitleWidgetService(
        "/title",
        errorHandlerWithSnackbar(enqueueSnackbar),
        false
    );
    const [content, setContent] = useState<string | undefined>(undefined);
    const [loading, setLoading] = useState<boolean>(false);

    useEffect(() => {
        async function fetchPreview() {
            if (open && !content) {
                setLoading(true);
                try {
                    const response = await service.getPreview(id);
                    setContent(response);
                } catch (error) {
                    console.error(`Failed to load preview: ${error}`);
                } finally {
                    setLoading(true);
                }
            }
        }

        fetchPreview();
    }, [open, id]);

    function handleClose() {
        setContent(undefined);
        setLoading(false);
        onClose();
    }

    return (
        <Dialog fullWidth maxWidth="md" open={open} onClose={handleClose}>
            <DialogTitle>Title preview</DialogTitle>
            <DialogContent>
                <Card sx={{ borderRadius: 0, minHeight: 200 }}>
                {loading && (
                    <Stack alignItems="center" sx={{ py: 3 }}>
                    <CircularProgress />
                    </Stack>
                )}
                {content && !loading && (
                    <object
                    type="image/svg+xml"
                    data={content}
                    style={{ width: "100%", display: "block" }}
                    aria-label="SVG Preview"
                    />
                )}
                </Card>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} autoFocus>
                OK
                </Button>
            </DialogActions>
        </Dialog>
    );
}

function TemplateEditor({
    value,
    onSubmit,
    onChange,
}: ValueEditorProps<string>): React.ReactElement {
    const [templates, setTeamplates] = useState<string[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const service = useTitleWidgetService("/title", undefined, false);

    useEffect(() => {
        async function fetchTemplate() {
            try {
                const fetchedTemplates = await service.getTemplates();
                setTeamplates(fetchedTemplates);
            } catch (error) {
                console.error(`Failed to load templates: ${error}`);
            } finally {
                setLoading(false);
            }
        }

        fetchTemplate();
    }, []);

    function handleChange(_event: unknown, newValue: string | null) {
        onChange(newValue || "");
    }

    return (
        <Box onSubmit={onSubmit} component="form">
            <Autocomplete
                disablePortal
                freeSolo
                sx={{ width: 1 }}
                size="small"
                value={value || ""}
                onChange={handleChange}
                options={templates}
                loading={loading}
                renderInput={(params) => (
                <TextField
                    {...params}
                    label="SVG preset"
                    InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                        <>
                        {loading && <CircularProgress color="inherit" size={20} />}
                        {params.InputProps.endAdornment}
                        </>
                    ),
                    }}
                />
                )}
            />
        </Box>
    );
}

function ParamsLine({ pKey, pValue }: ParamsLineProps): React.ReactElement {
    return (
        <Box component="div" sx={{ wordBreak: "break-word" }}>
            <b>{pKey}</b>: {String(pValue)}
        </Box>
    );
}

function ParamsDataEditor({
    onSubmit,
    value,
    onChange
}: ValueEditorProps<Record<string, unknown>>): React.ReactElement {
    const initialValue = paramsDataToString(value);
    const [inputValue, setInputValue] = useState<string>(initialValue);
    const debouncedInputValue = useDebounce(inputValue, 250);

    useEffect(() => {
        const parsedData = parseParamsData(debouncedInputValue);
        onChange(parsedData);
    }, [debouncedInputValue, onChange]);

    function handleChange(event: ChangeEvent<HTMLInputElement>) {
        setInputValue(event.target.value);
    }

    return (
        <Box onSubmit={onSubmit} component="form">
            <TextField
                autoFocus
                multiline
                hiddenLabel
                value={inputValue}
                placeholder="key: value"
                type="text"
                size="small"
                sx={{ width: 1 }}
                onChange={handleChange}
                minRows={2}
                maxRows={10}
            />
        </Box>
    );
}

function renderParamsData(data: Record<string, unknown>): React.ReactElement {
    const entries = Object.entries(data);

    if (entries.length === 0) {
        return <Box sx={{ color: 'text.secondary' }}>No parameters</Box>;
    }

    return (
        <Box>
            {entries.map(([key, value]) => (
                <ParamsLine key={key} pKey={key} pValue={value} />
            ))}
        </Box>
    );
}

function PresetSelectionCell({
    shown,
    onShow,
    onLeftPresetSelect,
    onRightPresetSelect
}: {
    shown: boolean;
    onShow: () => void;
    onLeftPresetSelect: () => void;
    onRightPresetSelect: () => void;
}): React.ReactElement {
    return (
        <TableCell component="th" scope="row" align="left">
            <Box display="flex" gap={0.5}>
                <ShowPresetButton
                    onClick={onShow}
                    checked={shown}
                />
                <IconButton
                    color="primary"
                    onClick={onLeftPresetSelect}
                    aria-label="Apply left preset"
                    size="small"
                >
                L
                </IconButton>
                <IconButton
                    color="primary"
                    onClick={onRightPresetSelect}
                    aria-label="Apply right preset"
                    size="small"
                >
                R
                </IconButton>
            </Box>
        </TableCell>
  );
}

function ActionButtonsCell({
    isEditMode,
    onPreviewOpen,
    onEditClick,
    onDeleteClick
}: {
    isEditMode: boolean;
    onPreviewOpen: () => void;
    onEditClick: () => void;
    onDeleteClick: () => void;
}): React.ReactElement {
    return (
        <TableCell component="th" scope="row" align="right">
            <Box display="flex" gap={0.5}>
                {!isEditMode && (
                    <IconButton
                        onClick={onPreviewOpen}
                        aria-label="Preview"
                        size="small"
                    >
                        <PreviewIcon />
                    </IconButton>
                )}
                <IconButton
                    color={isEditMode ? "primary" : "inherit"}
                    onClick={onEditClick}
                    aria-label={isEditMode ? "Save changes" : "Edit row"}
                    size="small"
                >
                    {isEditMode ? <SaveIcon /> : <EditIcon />}
                </IconButton>
                <IconButton
                    color="error"
                    onClick={onDeleteClick}
                    aria-label="Delete row"
                    size="small"
                >
                    <DeleteIcon />
                </IconButton>
            </Box>
        </TableCell>
  );
}

export function TitleTableRow({
    data,
    onShow,
    onEdit,
    onDelete
}: TitleTableRowProps): React.ReactElement {
    const {
        editData,
        onClickEdit,
        onSubmitEdit,
        onChangeField
    } = usePresetTableRowDataState(data, onEdit);

    const [previewDialogOpen, setPreviewDialogOpen] = useState<boolean>(false);
    const isEditMode = editData !== undefined;

    function handlePreviewOpen() {
        setPreviewDialogOpen(true);
    }

    function handlePreviewClose() {
        setPreviewDialogOpen(false);
    }

    const applyLeftPreset = createPresetUpdater(data, data.settings.leftPreset, onEdit);
    const applyRightPreset = createPresetUpdater(data, data.settings.rightPreset, onEdit);

    return (
        <>
            <PreviewSVGDialog
                open={previewDialogOpen}
                onClose={handlePreviewClose}
                id={data.id}
            />

            <TableRow
                key={data.id}
                sx={{ backgroundColor: data.shown ? activeRowColor : undefined }}
            >
                <PresetSelectionCell
                    shown={data.shown}
                    onShow={onShow}
                    onLeftPresetSelect={applyLeftPreset}
                    onRightPresetSelect={applyRightPreset}
                />

                <PresetsTableCell
                    value={data.settings.leftPreset}
                    editValue={editData?.settings?.leftPreset}
                    onChange={onChangeField("leftPreset")}
                    onSubmit={onSubmitEdit}
                    ValueEditor={TemplateEditor}
                />

                <PresetsTableCell
                    value={data.settings.rightPreset}
                    editValue={editData?.settings?.rightPreset}
                    onChange={onChangeField("rightPreset")}
                    onSubmit={onSubmitEdit}
                    ValueEditor={TemplateEditor}
                />

                <PresetsTableCell
                    value={data.settings.preset}
                    editValue={editData?.settings?.preset}
                    onChange={onChangeField("preset")}
                    onSubmit={onSubmitEdit}
                    ValueEditor={TemplateEditor}
                />

                <PresetsTableCell
                    value={data.settings.data}
                    editValue={editData?.settings?.data}
                    ValueEditor={ParamsDataEditor}
                    onChange={onChangeField("data")}
                    onSubmit={onSubmitEdit}
                    valuePrinter={renderParamsData}
                />

                <ActionButtonsCell
                    isEditMode={isEditMode}
                    onPreviewOpen={handlePreviewOpen}
                    onEditClick={onClickEdit}
                    onDeleteClick={onDelete}
                />
            </TableRow>
        </>
  );
}

export function Title(): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const service = usePresetWidgetService("/title", errorHandlerWithSnackbar(enqueueSnackbar));

    const defaultRowData: Partial<TitleSettings> = {
        preset: "",
        leftPreset: "",
        rightPreset: "",
        data: {}
    };

    return (
        <Container maxWidth="lg" sx={{ pt: 2 }} className="Title">
            <PresetsManager
                service={service}
                tableKeys={["leftPreset", "rightPreset", "preset", "data"]}
                tableKeysHeaders={["Left template", "Right template", "Template", "Data"]}
                defaultRowData={defaultRowData}
                RowComponent={TitleTableRow}
            />
        </Container>
    );
}

export type {
    TitleData,
    TitleSettings,
    TitleTableRowProps,
    PreviewSVGDialogProps,
    ParamsLineProps
};

export default Title;
