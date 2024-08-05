import { SlimTableCell } from "../atoms/Table.js";
import { Button, ButtonGroup, Switch, Table, TableBody, TableRow } from "@mui/material";
import { Dispatch, SetStateAction } from "react";
import { FullScreenClockSettings } from "../../../../generated/api.ts";
import { AbstractSingleWidgetService } from "@/services/abstractSingleWidget.ts";

export const DEFAULT_FULL_SCREEN_CLOCK_SETTINGS: FullScreenClockSettings = {
    globalTimeMode: false,
    quietMode: false,
    contestCountdownMode: false
};

export type FullScreenClockManagerProps = {
    service: AbstractSingleWidgetService<FullScreenClockSettings>;
    isShown: boolean;
    settings: FullScreenClockSettings;
    setSettings: Dispatch<SetStateAction<FullScreenClockSettings>>;
}

const FullScreenClockManager = ({ service, isShown, settings, setSettings }: FullScreenClockManagerProps) => {
    return (<>
        <Table align="center" size="small">
            <TableBody>
                <TableRow>
                    <SlimTableCell>
                        Global time instead contest
                    </SlimTableCell>
                    <SlimTableCell align={"center"}>
                        <Switch
                            checked={settings.globalTimeMode}
                            onChange={(e) => setSettings(s => ({ ...s, globalTimeMode: e.target.checked }))}
                        />
                    </SlimTableCell>
                </TableRow>
                <TableRow>
                    <SlimTableCell>
                        Contest time countdown
                    </SlimTableCell>
                    <SlimTableCell align={"center"}>
                        <Switch
                            checked={settings.contestCountdownMode}
                            onChange={(e) => setSettings(s => ({ ...s, contestCountdownMode: e.target.checked }))}
                        />
                    </SlimTableCell>
                </TableRow>
                <TableRow>
                    <SlimTableCell>
                        Quiet mode (seconds only in countdown)
                    </SlimTableCell>
                    <SlimTableCell align={"center"}>
                        <Switch
                            checked={settings.quietMode}
                            onChange={(e) => setSettings(s => ({ ...s, quietMode: e.target.checked }))}
                        />
                    </SlimTableCell>
                </TableRow>
            </TableBody>
        </Table>
        <div>
            <ButtonGroup variant="contained" sx={{ m: 2 }}>
                <Button color="primary" onClick={() => service.showWithSettings(settings)}>Show</Button>
                <Button color="error" disabled={!isShown} onClick={() => service.hide()}>Hide</Button>
            </ButtonGroup>
        </div>
    </>);
};

export default FullScreenClockManager;
