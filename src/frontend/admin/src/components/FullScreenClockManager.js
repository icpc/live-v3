import { SlimTableCell } from "./atoms/Table";
import { Button, ButtonGroup, Container, Switch, Table, TableBody, TableRow } from "@mui/material";
import { useSnackbar } from "notistack";
import React, { useEffect, useState } from "react";
import { errorHandlerWithSnackbar } from "../errors";
import { useFullScreenClockWidget } from "../services/fullScreenClockWidget";


function FullScreenClockManager() {
    const { enqueueSnackbar,  } = useSnackbar();
    const service = useFullScreenClockWidget();
    useEffect(() => {
        const h = errorHandlerWithSnackbar(enqueueSnackbar);
        service.addErrorHandler(h);
        return () => service.deleteErrorHandler(h);
    }, [service, enqueueSnackbar]);

    const [isShown, setIsShown] = useState(false);
    const [settings, setSettings] = useState({ globalTimeMode: false, quietMode: false });
    const loadSettings = () => service.loadOne().then((info) => setIsShown(info.shown));

    useEffect(loadSettings, []);
    useEffect(() => {
        service.addReloadDataHandler(loadSettings);
        return () => service.deleteReloadDataHandler(loadSettings);
    }, []);

    return (<Container maxWidth="md" sx={{ display: "flex", flexDirection: "column", pt: 2 }}>
        <Table align="center" sx={{ my: 2 }} size="small">
            <TableBody>
                <TableRow>
                    <SlimTableCell align={"center"}>
                        Global time instead contest
                        <Switch checked={settings.globalTimeMode}
                            onChange={(e) => setSettings(s => ({ ...s, globalTimeMode: e.target.checked }))}/>
                    </SlimTableCell>
                    <SlimTableCell align={"center"}>
                        <ButtonGroup variant="contained" sx={{ m: 2 }}>
                            <Button color="primary" onClick={() => service.showPresetWithSettings(null, settings)}>Show</Button>
                            <Button color="error" disabled={!isShown} onClick={() => service.hidePreset()}>Hide</Button>
                        </ButtonGroup>
                    </SlimTableCell>
                </TableRow>
                <TableRow>
                    <SlimTableCell align={"center"}>
                        Quiet mode (seconds only in countdown)
                        <Switch checked={settings.quietMode}
                            onChange={(e) => setSettings(s => ({ ...s, quietMode: e.target.checked }))}/>
                    </SlimTableCell>
                    <SlimTableCell align={"center"}>
                        <ButtonGroup variant="contained" sx={{ m: 2 }}>
                            <Button color="primary" onClick={() => service.showPresetWithSettings(null, settings)}>Show</Button>
                            <Button color="error" disabled={!isShown} onClick={() => service.hidePreset()}>Hide</Button>
                        </ButtonGroup>
                    </SlimTableCell>
                </TableRow>
            </TableBody>
        </Table>
    </Container>);
}

export default FullScreenClockManager;
