import React, { useState, FormEvent, ChangeEvent } from "react";
import { DateTime } from "luxon";
import {
  TableCell,
  TableRow,
  TextField,
  Select,
  MenuItem,
  FormControl,
  Switch,
  FormControlLabel,
  SelectChangeEvent
} from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import ImageIcon from "@mui/icons-material/Image";
import ShowPresetButton from "./controls/ShowPresetButton";
import { activeRowColor } from "../styles";
import { ClockType } from "../../../generated/api";

type TickerType = "clock" | "scoreboard" | "text" | "image";

interface BaseTickerSettings {
    type: TickerType;
    periodMs: number;
}

interface ClockSettings extends BaseTickerSettings {
    type: "clock";
    clockType?: ClockType;
    showSeconds?: boolean;
    timeZone?: string | null;
}

interface ScoreboardSettings extends BaseTickerSettings {
    type: "scoreboard";
    from: number;
    to: number;
}

interface TextSettings extends BaseTickerSettings {
    type: "text";
    text: string;
}

interface ImageSettings extends BaseTickerSettings {
    type: "image";
    path: string;
}

type TickerSettings = ClockSettings | ScoreboardSettings | TextSettings | ImageSettings;

interface TickerData {
    id: string | number;
    shown: boolean;
    settings: TickerSettings;
}

interface TickerTableRowProps {
    data: TickerData;
    onShow: () => void;
    onEdit: (data: TickerData) => unknown;
    onDelete: () => void;
}

function getTickerIcon(type: TickerType): React.ReactElement {
    const iconMap: Record<string, React.ReactElement> = {
        clock: <ClockIcon />,
        scoreboard: <ScoreboardIcon />,
        text: <TextIcon />,
        image: <ImageIcon />,
    };

    return iconMap[type];
}

function formatClockDisplay(settings: ClockSettings): string {
    const clockType = settings.clockType || ClockType.standard;
    const secondsText = settings.showSeconds !== false ? "w/ seconds" : "no seconds";
    const timeZoneText = settings.timeZone ? ` (${settings.timeZone})` : "";
    return `${clockType} ${secondsText}${timeZoneText}`;
}

function formatScoreboardDisplay(settings: ScoreboardSettings): string {
    return `From ${settings.from} to ${settings.to}`;
}

function validateTimeZone(timeZone: string): { isValid: boolean, errorMessage: string } {
    if (timeZone === "") {
        return { isValid: true, errorMessage: "" };
    }

    const dateTime = DateTime.now().setZone(timeZone);
    return {
        isValid: dateTime.isValid,
        errorMessage: dateTime.isValid ? "" : (dateTime.invalidReason || "Invalid timezone"),
    };
}

function useTickerEditState(data: TickerData, onEdit: (data: TickerData) => unknown) {
    const [editData, setEditData] = useState<TickerData | undefined>(undefined);
    const [timeZoneError, setTimeZoneError] = useState<string>("");

    function isEditMode(): boolean {
        return editData !== undefined;
    }

    function startEdit(): void {
        setEditData({
            ...data,
            settings: { ...data.settings }
        });
        setTimeZoneError("");
    }

    async function saveEdit() {
        if (editData && !timeZoneError) {
            await onEdit(editData);
            setEditData(undefined);
            setTimeZoneError("");
        }
    }

    function handleEditToggle(): void {
        if (isEditMode()) {
            saveEdit();
        } else {
            startEdit();
        }
    }

    function handleSubmit(event: FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        handleEditToggle();
    }

    function updateField(field: string, value: unknown): void {
        if (!editData) return;

        setEditData({
            ...editData,
            settings: {
                ...editData.settings,
                [field]: value
            }
        });
    }

    function handleFieldChange(field: string): (event: ChangeEvent<HTMLInputElement>) => void {
        return function (event: ChangeEvent<HTMLInputElement>): void {
            const value = event.target.type === 'number' ? Number(event.target.value) : event.target.value;
            updateField(field, value);
        };
    }

    function handleTimeZoneChange(event: ChangeEvent<HTMLInputElement>): void {
        const value = event.target.value;
        const validation = validateTimeZone(value);

        setTimeZoneError(validation.errorMessage);
        if (validation.isValid) {
            updateField('timeZone', value || null);
        }
    }

    return {
        editData,
        timeZoneError,
        isEditMode: isEditMode(),
        handleEditToggle,
        handleSubmit,
        updateField,
        handleFieldChange,
        handleTimeZoneChange
    };
}

function ClockEditor({
    data,
    editData,
    timeZoneError,
    onSubmit,
    onFieldChange,
    onTimeZoneChange
}: {
    data: TickerData;
    editData: TickerData;
    timeZoneError: string;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    onFieldChange: (field: string, value: unknown) => void;
    onTimeZoneChange: (event: ChangeEvent<HTMLInputElement>) => void;
}): React.ReactElement {
    const settings = editData.settings as ClockSettings;

    function handleClockTypeChange(event: SelectChangeEvent): void {
        onFieldChange('clockType', event.target.value);
    }

    function handleShowSecondsChange(event: ChangeEvent<HTMLInputElement>): void {
        onFieldChange('showSeconds', event.target.checked);
    }
    return (
        <Box onSubmit={onSubmit} component="form" sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            <FormControl size="small" fullWidth>
                <Select
                    value={settings.clockType || ClockType.standard}
                    onChange={handleClockTypeChange}
                >
                    <MenuItem value={ClockType.standard}>Standard</MenuItem>
                    <MenuItem value={ClockType.countdown}>Countdown</MenuItem>
                    <MenuItem value={ClockType.global}>Global</MenuItem>
                </Select>
            </FormControl>
            <FormControlLabel
                control={
                    <Switch
                        checked={settings.showSeconds !== false}
                        onChange={handleShowSecondsChange}
                        size="small"
                    />
                }
                label="Show seconds"
            />
            {settings.clockType === ClockType.global && (
                <TextField
                    size="small"
                    placeholder="Timezone"
                    defaultValue={(data.settings as ClockSettings).timeZone || ""}
                    error={timeZoneError !== ""}
                    helperText={timeZoneError}
                    onChange={onTimeZoneChange}
                />
            )}
        </Box>
    );
}

function TextEditor({
    data,
    onSubmit,
    onChange
}: {
    data: TickerData;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}): React.ReactElement {
    return (
        <Box onSubmit={onSubmit} component="form">
            <TextField
                autoFocus
                hiddenLabel
                fullWidth
                defaultValue={(data.settings as TextSettings).text}
                type="text"
                size="small"
                sx={{ width: 1 }}
                onChange={onChange}
            />
        </Box>
    );
}

function ImageEditor({
    data,
    onSubmit,
    onChange
}: {
    data: TickerData;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}): React.ReactElement {
    return (
        <Box onSubmit={onSubmit} component="form">
            <TextField
                autoFocus
                hiddenLabel
                fullWidth
                defaultValue={(data.settings as ImageSettings).path}
                type="text"
                size="small"
                sx={{ width: 1 }}
                onChange={onChange}
            />
        </Box>
    );
}

function ScoreboardEditor({
    data,
    onSubmit,
    onFromChange,
    onToChange
}: {
    data: TickerData;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    onFromChange: (event: ChangeEvent<HTMLInputElement>) => void;
    onToChange: (event: ChangeEvent<HTMLInputElement>) => void;
}): React.ReactElement {
    const settings = data.settings as ScoreboardSettings;

    return (
        <Box onSubmit={onSubmit} component="form" sx={{ display: "flex", flexDirection: "row", gap: 1 }}>
            <TextField
                autoFocus
                hiddenLabel
                fullWidth
                defaultValue={settings.from}
                type="number"
                size="small"
                sx={{ flex: 1 }}
                onChange={onFromChange}
            />
            <TextField
                hiddenLabel
                fullWidth
                defaultValue={settings.to}
                type="number"
                size="small"
                sx={{ flex: 1 }}
                onChange={onToChange}
            />
        </Box>
    );
}

function PeriodEditor({
    periodMs,
    onSubmit,
    onChange
}: {
    periodMs: number;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}): React.ReactElement {
    return (
        <Box onSubmit={onSubmit} component="form">
            <TextField
                autoFocus
                hiddenLabel
                defaultValue={periodMs}
                type="number"
                size="small"
                onChange={onChange}
            />
        </Box>
    );
}

export function TickerTableRow({
    data,
    onShow,
    onEdit,
    onDelete
}: TickerTableRowProps): React.ReactElement {
    const {
        editData,
        timeZoneError,
        isEditMode,
        handleEditToggle,
        handleSubmit,
        updateField,
        handleFieldChange,
        handleTimeZoneChange
    } = useTickerEditState(data, onEdit);

    function renderSettingsContent(): React.ReactElement {
        const { type } = data.settings;

        if (!isEditMode) {
            switch (type) {
                case "clock":
                    return <>{formatClockDisplay(data.settings as ClockSettings)}</>;
                case "text":
                    return <>{(data.settings as TextSettings).text}</>;
                case "image":
                    return <>{(data.settings as ImageSettings).path}</>;
                case "scoreboard":
                    return <>{formatScoreboardDisplay(data.settings as ScoreboardSettings)}</>;
            }
        }

        switch (type) {
            case "clock":
                return (
                    <ClockEditor
                        data={data}
                        editData={editData!}
                        timeZoneError={timeZoneError}
                        onSubmit={handleSubmit}
                        onFieldChange={updateField}
                        onTimeZoneChange={handleTimeZoneChange}
                    />
                );
            case "text":
                return (
                    <TextEditor
                        data={data}
                        onSubmit={handleSubmit}
                        onChange={handleFieldChange("text")}
                    />
                );
            case "image":
                return (
                    <ImageEditor
                        data={data}
                        onSubmit={handleSubmit}
                        onChange={handleFieldChange("path")}
                    />
                );
            case "scoreboard":
                return (
                    <ScoreboardEditor
                        data={data}
                        onSubmit={handleSubmit}
                        onFromChange={handleFieldChange("from")}
                        onToChange={handleFieldChange("to")}
                    />
                );
        }
    }

    return (
        <TableRow
            key={data.id}
            sx={{ backgroundColor: data.shown ? activeRowColor : undefined }}
        >
            <TableCell component="th" scope="row" align="left">
                <ShowPresetButton onClick={onShow} checked={data.shown} />
            </TableCell>

            <TableCell component="th" scope="row">
                {getTickerIcon(data.settings.type)}
            </TableCell>

            <TableCell component="th" scope="row">
                {renderSettingsContent()}
            </TableCell>

            <TableCell component="th" scope="row">
                {!isEditMode ? (
                    data.settings.periodMs
                ) : (
                    <PeriodEditor
                        periodMs={data.settings.periodMs}
                        onSubmit={handleSubmit}
                        onChange={handleFieldChange('periodMs')}
                    />
                )}
            </TableCell>

            <TableCell component="th" scope="row" align="right">
                <Box>
                    <IconButton
                        color={isEditMode ? "primary" : "inherit"}
                        onClick={handleEditToggle}
                        aria-label={isEditMode ? "Save changes" : "Edit row"}
                    >
                        {isEditMode ? <SaveIcon /> : <EditIcon />}
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
        </TableRow>
    );
}

export type {
    TickerData,
    TickerSettings,
    TickerTableRowProps,
    TickerType,
    ClockSettings,
    ScoreboardSettings,
    TextSettings,
    ImageSettings
};
