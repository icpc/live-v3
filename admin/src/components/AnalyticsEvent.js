import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import CommentIcon from "@mui/icons-material/Comment";
import {
    Box,
    Button,
    ButtonGroup,
    createTheme,
    Grid,
    Table,
    TableBody,
    TableCell,
    TableRow,
    ThemeProvider
} from "@mui/material";
import _ from "lodash";
import PropTypes from "prop-types";
import React, { useEffect, useRef, useState } from "react";
import { BASE_URL_WS } from "../config";
import { PresetWidgetService } from "../services/presetWidget";
import { activeRowColor, selectedRowColor } from "../styles";
import { timeMsToDuration } from "../utils";

const apiUrl = () => {
    return BASE_URL_WS + "/analyticsEvents";
};

const rowTheme = createTheme({
    components: {
        MuiTableCell: {
            styleOverrides: {
                // Name of the slot
                root: {
                    // Some CSS
                    lineHeight: "100%",
                    padding: "2px 6px"
                },
            },
            variants: [{
                props: {
                    type: "icon"
                },
                style: {
                    fontSize: "0px"
                }
            }]
        },
    },
});

function EventsTable({ events, selectedRowId, onRowClick }) {
    const rowBackground = e => e.id === selectedRowId ? selectedRowColor :
        (e._advertisementId !== undefined || e._tickerMsgId !== undefined ? activeRowColor : undefined);
    const data = events.map(e => ({ ...e, _rowBackground: rowBackground(e) }));
    return (
        <ThemeProvider theme={rowTheme}>
            <Table sx={{ m: 2 }} size="small">
                <TableBody>
                    {data.map((event, rowId) =>
                        <TableRow key={rowId} sx={{ backgroundColor: event._rowBackground, cursor: "pointer" }}
                            onClick={() => onRowClick(event.id)}>
                            <TableCell type="icon">{event.type === "commentary" ? <CommentIcon/> : "???"}</TableCell>
                            <TableCell>{event.type === "commentary" ? event.message : ""}</TableCell>
                            <TableCell>{timeMsToDuration(event.timeMs)}</TableCell>
                        </TableRow>)}
                </TableBody>
            </Table>
        </ThemeProvider>);
}

EventsTable.propTypes = {
    events: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.any.isRequired,
            type: PropTypes.string.isRequired,
        }).isRequired).isRequired,
    selectedRowId: PropTypes.any,
    onRowClick: PropTypes.func.isRequired,
};

function Analytics() {
    const [events, setEvents] = useState([]);
    const [selectedEventId, setSelectedEventId] = useState();
    const ws = useRef(null);

    const selectedEvent = events.find(e => e.id === selectedEventId);
    const deselectEvent = () => setSelectedEventId(undefined);

    const advertisementService = new PresetWidgetService("/advertisement", console.log);
    const tickerService = new PresetWidgetService("/tickerMessage", console.log);
    useEffect(() => {
        ws.current = new WebSocket(apiUrl());

        ws.current.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setEvents((_messages) => [data, ..._messages]);
        };

        return () => {
            ws.current?.close();
        };
    }, []);

    function createAndShowAdvertisement() {
        const presetSettings = { text: selectedEvent.message };
        advertisementService.createPreset(presetSettings)
            .then(() => advertisementService.loadPresets())
            .then(ps => ps.findLast(p => _.isEqual(p.settings, presetSettings))?.id)
            .then(presetId => advertisementService.showPreset(presetId).then(() => presetId))
            .then((presetId) => selectedEvent._advertisementId = presetId)
            .then(deselectEvent);
    }

    function hideAdvertisement() {
        advertisementService.deletePreset(selectedEvent._advertisementId)
            .then(() => selectedEvent._advertisementId = undefined)
            .then(deselectEvent);
    }

    function createAndShowTickerMessage() {
        const presetSettings = { text: selectedEvent.message, periodMs: 30000, type: "text", part: "long" };
        tickerService.createPreset(presetSettings)
            .then(() => tickerService.loadPresets())
            .then(ps => ps.findLast(p => _.isEqual(p.settings, presetSettings))?.id)
            .then(presetId => tickerService.showPreset(presetId).then(() => presetId))
            .then((presetId) => selectedEvent._tickerMsgId = presetId)
            .then(deselectEvent);
    }

    function hideTickerMessage() {
        tickerService.deletePreset(selectedEvent._tickerMsgId)
            .then(() => selectedEvent._tickerMsgId = undefined)
            .then(deselectEvent);
    }


    const scrollTarget = useRef(null);
    React.useEffect(() => {
        if (scrollTarget.current) {
            scrollTarget.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [events.length]);

    // const selectedEventMessage = selectedEvent?.type === "commentary" ? selectedEvent?.message : undefined;
    // const selectedEventTeam = selectedEvent?.type === "commentary" ? selectedEvent?.team : undefined;

    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column"
    }}>
        <Box sx={{ width: { "md": "75%", "sm": "100%", "xs": "100%" } }}>
            <Box sx={{ pt: 2 }}>
                <ButtonGroup disabled={selectedEvent?.message === undefined}>
                    <Button color="primary" variant={"outlined"} startIcon={<ArrowForwardIcon/>}
                        disabled={selectedEvent?._advertisementId !== undefined}
                        onClick={createAndShowAdvertisement}>advertisement</Button>
                    <Button color="error" disabled={selectedEvent?._advertisementId === undefined}
                        onClick={hideAdvertisement}>Hide</Button>
                </ButtonGroup>

                <ButtonGroup disabled={selectedEvent?.message === undefined}>
                    <Button color="primary" variant={"outlined"} startIcon={<ArrowForwardIcon/>}
                        disabled={selectedEvent?._tickerMsgId !== undefined}
                        onClick={createAndShowTickerMessage}>ticker</Button>
                    <Button color="error" disabled={selectedEvent?._tickerMsgId === undefined}
                        onClick={hideTickerMessage}>Hide</Button>
                </ButtonGroup>

                {/*<Button color="primary" variant={"outlined"}*/}
                {/*    onClick={() => {}}>To ticker</Button>*/}
                {/*<Tooltip title="Show on ... s">*/}
                {/*    <TextField*/}
                {/*        hiddenLabel*/}
                {/*        defaultValue={5*60}*/}
                {/*        id="filled-hidden-label-small"*/}
                {/*        type="text"*/}
                {/*        size="small"*/}
                {/*        sx={{ width: 100 }}*/}
                {/*        disabled={selectedEventMessage === undefined}*/}
                {/*        onChange={(e) => {e.target.value;}}*/}
                {/*    />*/}
                {/*</Tooltip>*/}


                {/*Team: <TeamViewSettingsPanel isSomethingSelected={selectedEventTeam !== undefined} showTeamFunction={() => {}}*/}
                {/*    hideTeamFunction={() => {}} isPossibleToHide={false}/>*/}
            </Box>
            <EventsTable events={events} selectedRowId={selectedEventId}
                onRowClick={(id) => setSelectedEventId(prevId => id === prevId ? undefined : id)}/>
        </Box>
    </Grid>
    );
}

export default Analytics;
