import { SlimTableCell } from "../atoms/Table.js";
import {
    Button,
    ButtonGroup,
    Switch,
    Table,
    TableBody,
    TableRow,
    Radio,
    RadioGroup,
    FormControlLabel,
    FormControl,
    FormLabel,
    TextField,
} from "@mui/material";
import { Dispatch, SetStateAction } from "react";
import { FullScreenClockSettings, ClockType } from "@shared/api.ts";
import { AbstractSingleWidgetService } from "@/services/abstractSingleWidget.ts";

export const DEFAULT_FULL_SCREEN_CLOCK_SETTINGS: FullScreenClockSettings = {
    clockType: ClockType.standard,
    showSeconds: true,
    timeZone: null,
};

export type FullScreenClockManagerProps = {
    service: AbstractSingleWidgetService<FullScreenClockSettings>;
    isShown: boolean;
    settings: FullScreenClockSettings;
    setSettings: Dispatch<SetStateAction<FullScreenClockSettings>>;
};

const FullScreenClockManager = ({
    service,
    isShown,
    settings,
    setSettings,
}: FullScreenClockManagerProps) => {
    return (
        <>
            <Table align="center" size="small">
                <TableBody>
                    <TableRow>
                        <SlimTableCell>
                            <FormControl component="fieldset">
                                <FormLabel component="legend">
                                    Clock Type
                                </FormLabel>
                                <RadioGroup
                                    value={settings.clockType}
                                    onChange={(e) =>
                                        setSettings((s) => ({
                                            ...s,
                                            clockType: e.target
                                                .value as ClockType,
                                        }))
                                    }
                                >
                                    <FormControlLabel
                                        value={ClockType.standard}
                                        control={<Radio />}
                                        label="Standard (countdown → contest time → over)"
                                    />
                                    <FormControlLabel
                                        value={ClockType.countdown}
                                        control={<Radio />}
                                        label="Countdown (countdown to start → countdown to finish → over)"
                                    />
                                    <FormControlLabel
                                        value={ClockType.global}
                                        control={<Radio />}
                                        label="Global time (timezone based)"
                                    />
                                </RadioGroup>
                            </FormControl>
                        </SlimTableCell>
                    </TableRow>
                    <TableRow>
                        <SlimTableCell>Show seconds</SlimTableCell>
                        <SlimTableCell align={"center"}>
                            <Switch
                                checked={settings.showSeconds}
                                onChange={(e) =>
                                    setSettings((s) => ({
                                        ...s,
                                        showSeconds: e.target.checked,
                                    }))
                                }
                            />
                        </SlimTableCell>
                    </TableRow>
                    {settings.clockType === ClockType.global && (
                        <TableRow>
                            <SlimTableCell>Timezone</SlimTableCell>
                            <SlimTableCell align={"center"}>
                                <TextField
                                    value={settings.timeZone || ""}
                                    onChange={(e) =>
                                        setSettings((s) => ({
                                            ...s,
                                            timeZone: e.target.value || null,
                                        }))
                                    }
                                    placeholder="e.g., Europe/London, America/New_York"
                                    size="small"
                                    fullWidth
                                />
                            </SlimTableCell>
                        </TableRow>
                    )}
                </TableBody>
            </Table>
            <div>
                <ButtonGroup variant="contained" sx={{ m: 2 }}>
                    <Button
                        color="primary"
                        onClick={() => service.showWithSettings(settings)}
                    >
                        Show
                    </Button>
                    <Button
                        color="error"
                        disabled={!isShown}
                        onClick={() => service.hide()}
                    >
                        Hide
                    </Button>
                </ButtonGroup>
            </div>
        </>
    );
};

export default FullScreenClockManager;
