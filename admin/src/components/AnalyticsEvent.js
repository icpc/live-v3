import React, { useEffect, useState, useRef } from "react";
import { BASE_URL_WS } from "../config";
import { Box, Button, ButtonGroup, Grid, Table, TableBody, TableCell, TableRow, TextField, Tooltip } from "@mui/material";
import CommentIcon from "@mui/icons-material/Comment";
import { timeMsToDuration } from "../utils";
import { lightBlue } from "@mui/material/colors";
import PropTypes from "prop-types";
import { ChooseMediaTypeAndShowPanel } from "./TeamTable";

const apiUrl = () => {
    return BASE_URL_WS + "/analyticsEvents";
};

function EventsTable({ events, selectedRowId, onRowClick }) {
    const data = events.map(e => ({ ...e, rowBackground: (e.id === selectedRowId ? lightBlue[100] : "inherit") }));
    return (<Table sx={{ m: 2 }} size="small">
        <TableBody>
            {data.map((event, rowId) =>
                <TableRow key={rowId} sx={{ backgroundColor: event.rowBackground, cursor: "pointer" }}
                    onClick={() => onRowClick(event.id)}>
                    <TableCell>{event.type === "commentary" ? <CommentIcon/> : "???"}</TableCell>
                    <TableCell>{event.type === "commentary" ? event.message : ""}</TableCell>
                    <TableCell>{timeMsToDuration(event.timeMs)}</TableCell>
                </TableRow>)}
        </TableBody>
    </Table>);
}
EventsTable.propTypes = {
    events: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.any.isRequired,
            type: PropTypes.string.isRequired,
        }).isRequired).isRequired,
    selectedRowId: PropTypes.any.isRequired,
    onRowClick: PropTypes.func.isRequired,
};

function Analytics() {
    const [events, setEvents] = useState([]);
    const [selectedEventId, setSelectedEventId] = useState();
    const ws = useRef(null);

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

    const scrollTarget = useRef(null);

    React.useEffect(() => {
        if (scrollTarget.current) {
            scrollTarget.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [events.length]);

    const selectedEvent = events.find(e => e.id === selectedEventId);
    const selectedEventMessage = selectedEvent?.type === "commentary" ? selectedEvent?.message : undefined;
    const selectedEventTeam = selectedEvent?.type === "commentary" ? selectedEvent?.team : undefined;

    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column"
    }}>
        <Box sx={{ width: { "md": "75%", "sm": "100%", "xs": "100%" } }}>
            <Box>
                Message: <ButtonGroup variant="outlined" sx={{ m: 2 }} disabled={selectedEventMessage === undefined}>
                    <Button color="primary" variant={"outlined"}
                        onClick={() => {}}>Show advertisement</Button>
                    <Button color="error" disabled={true} onClick={() => {}}>Hide</Button>
                    <Button color="primary" variant={"outlined"}
                        onClick={() => {}}>To ticker</Button>
                    <Tooltip title="Show on ... s">
                        <TextField
                            hiddenLabel
                            defaultValue={5*60}
                            id="filled-hidden-label-small"
                            type="text"
                            size="small"
                            sx={{ width: 100 }}
                            disabled={selectedEventMessage === undefined}
                            onChange={(e) => {e.target.value;}}
                        />
                    </Tooltip>
                </ButtonGroup>

                Team: <ChooseMediaTypeAndShowPanel isSomethingSelected={selectedEventTeam !== undefined} showTeamFunction={() => {}}
                    hideTeamFunction={() => {}} isPossibleToHide={false}/>
            </Box>
            <EventsTable events={events} selectedRowId={selectedEventId} onRowClick={(id) => setSelectedEventId(prevId => id === prevId ? undefined : id)}/>
        </Box>
    </Grid>
    );
}

export default Analytics;
