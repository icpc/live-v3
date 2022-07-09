import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import CommentIcon from "@mui/icons-material/Comment";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { BASE_URL_WS } from "../config";
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
    TextField,
    ThemeProvider
} from "@mui/material";
import PropTypes from "prop-types";
import { PresetWidgetService } from "../services/presetWidget";
import { activeRowColor, selectedAndActiveRowColor, selectedRowColor } from "../styles";
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

const isActive = (e) => {
    return e._advertisementId !== undefined || e._tickerMsgId !== undefined;
};

function EventsTable({ events, selectedRowId, onRowClick }) {
    const rowBackground = useCallback((e) => {
        if (e.id === selectedRowId) {
            return isActive(e) ? selectedAndActiveRowColor : selectedRowColor;
        } else {
            return isActive(e) ? activeRowColor : undefined;
        }
    }, [selectedRowId]);
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

    const advertisementService = useMemo(() => new PresetWidgetService("/advertisement", console.log), []);
    const tickerService = useMemo(() => new PresetWidgetService("/tickerMessage", console.log), []);
    const [advertisementTtl, setAdvertisementTtl] = useState(30);
    const [tickerMsgTtl, setTickerMsgTtl] = useState(120);

    useEffect(() => {
        ws.current = new WebSocket(apiUrl());

        ws.current.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setEvents((_events) => [ data, ..._events ]);
        };

        return () => {
            ws.current?.close();
        };
    }, []);

    function createAndShowAdvertisement() {
        const presetSettings = { text: selectedEvent.message };
        advertisementService.createAndShowWithTtl(presetSettings, advertisementTtl * 1000)
            .then(r => {
                selectedEvent._advertisementId = r.response;
                selectedEvent._advertisementHideTimer = setTimeout(() => {
                    selectedEvent._advertisementId = undefined;
                    setEvents(es => [ ...es ]);
                }, advertisementTtl * 1000);
                deselectEvent();
            });
    }

    function hideAdvertisement() {
        advertisementService.deletePreset(selectedEvent._advertisementId)
            .then(() => {
                selectedEvent._advertisementId = undefined;
                if (selectedEvent._advertisementHideTimer !== undefined) {
                    clearTimeout(selectedEvent._advertisementHideTimer);
                }
                deselectEvent();
            });
    }

    function createAndShowTickerMessage() {
        const presetSettings = { text: selectedEvent.message, periodMs: 30000, type: "text", part: "long" };
        tickerService.createAndShowWithTtl(presetSettings, tickerMsgTtl * 1000)
            .then(r => {
                selectedEvent._tickerMsgId = r.response;
                selectedEvent._tickerMsgHideTimer = setTimeout(() => {
                    selectedEvent._tickerMsgId = undefined;
                    setEvents(es => [ ...es ]);
                }, tickerMsgTtl * 1000);
                deselectEvent();
            });
    }

    function hideTickerMessage() {
        tickerService.deletePreset(selectedEvent._tickerMsgId)
            .then(() => {
                selectedEvent._tickerMsgId = undefined;
                if (selectedEvent._tickerMsgHideTimer !== undefined) {
                    clearTimeout(selectedEvent._tickerMsgHideTimer);
                }
                deselectEvent();
            });
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
                <ButtonGroup disabled={selectedEvent?.message === undefined} sx={{ px: 2 }}>
                    <Button color="primary" variant={"outlined"} startIcon={<ArrowForwardIcon/>}
                        disabled={selectedEvent?._advertisementId !== undefined}
                        onClick={createAndShowAdvertisement}>advertisement</Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setAdvertisementTtl(e.target.value)}
                        value={advertisementTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "36.5px" } }}
                    />
                    <Button color="error" disabled={selectedEvent?._advertisementId === undefined}
                        onClick={hideAdvertisement}>Hide</Button>
                </ButtonGroup>

                <ButtonGroup disabled={selectedEvent?.message === undefined}>
                    <Button color="primary" variant={"outlined"} startIcon={<ArrowForwardIcon/>}
                        disabled={selectedEvent?._tickerMsgId !== undefined}
                        onClick={createAndShowTickerMessage}>ticker</Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setTickerMsgTtl(e.target.value)}
                        value={tickerMsgTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "36.5px" } }}
                    />
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
