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
    return (
        <ThemeProvider theme={rowTheme}>
            <Table sx={{ m: 2 }} size="small">
                <TableBody>
                    {events.map((event, rowId) =>
                        <TableRow key={rowId} sx={{ backgroundColor: rowBackground(event), cursor: "pointer" }}
                            onClick={() => onRowClick(event)}>
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

export function useDebounceList(delay) {
    const [addCache, setAddCache] = useState([]);
    const [debouncedValue, setDebouncedValue] = useState([]);
    const add = (value) => setAddCache(cache => [ value, ...cache ]);
    useEffect(() => {
        const handler = setTimeout(() => setDebouncedValue(value => [ ...addCache, ...value ]), delay);
        return () => clearTimeout(handler);
    }, [addCache]);
    return [debouncedValue, setDebouncedValue, add];
}

function Analytics() {
    const [events, setEvents, addEvent] = useDebounceList(100);

    const [selectedEvent, setSelectedEvent] = useState();
    const ws = useRef(null);

    const updateEventById = (id, modifier) => setEvents(events => events.map(e => e.id === id ? modifier(e) : e));

    const advertisementService = useMemo(() => new PresetWidgetService("/advertisement", console.log), []);
    const tickerService = useMemo(() => new PresetWidgetService("/tickerMessage", console.log), []);
    const [advertisementTtl, setAdvertisementTtl] = useState(30);
    const [tickerMsgTtl, setTickerMsgTtl] = useState(120);

    useEffect(() => {
        ws.current = new WebSocket(apiUrl());

        ws.current.onmessage = (event) => {
            addEvent(JSON.parse(event.data));
        };

        return () => {
            ws.current?.close();
        };
    }, []);

    function createAndShowAdvertisement() {
        const presetSettings = { text: selectedEvent.message };
        advertisementService.createAndShowWithTtl(presetSettings, advertisementTtl * 1000)
            .then(r => updateEventById(selectedEvent.id, e => {
                e._advertisementId = r.response;
                r._advertisementHideTimer = setTimeout(() => {
                    updateEventById(e.id, e => { e._advertisementId = undefined; return e; });
                }, advertisementTtl * 1000);
                return e;
            }));
    }

    function hideAdvertisement() {
        advertisementService.deletePreset(selectedEvent._advertisementId)
            .then(() => updateEventById(selectedEvent.id, e => {
                e._advertisementId = undefined;
                if (e._advertisementHideTimer !== undefined) {
                    clearTimeout(e._advertisementHideTimer);
                }
                return e;
            }));
    }

    function createAndShowTickerMessage() {
        const presetSettings = { text: selectedEvent.message, periodMs: 30000, type: "text", part: "long" };
        tickerService.createAndShowWithTtl(presetSettings, tickerMsgTtl * 1000)
            .then(r => updateEventById(selectedEvent.id, e => {
                e._tickerMsgId = r.response;
                e._tickerMsgHideTimer = setTimeout(() => {
                    updateEventById(e.id, e => { e._tickerMsgId = undefined; return e; });
                }, tickerMsgTtl * 1000);
                return e;
            }));
    }

    function hideTickerMessage() {
        tickerService.deletePreset(selectedEvent._tickerMsgId)
            .then(() => updateEventById(selectedEvent.id, e => {
                e._tickerMsgId = undefined;
                if (e._tickerMsgHideTimer !== undefined) {
                    clearTimeout(e._tickerMsgHideTimer);
                }
                return e;
            }));
    }


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
            <EventsTable events={events} selectedRowId={selectedEvent?.id}
                onRowClick={(event) => setSelectedEvent(prevEvent => event.id === prevEvent?.id ? undefined : event)}/>
        </Box>
    </Grid>
    );
}

export default Analytics;
