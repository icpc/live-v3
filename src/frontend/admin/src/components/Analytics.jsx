import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import React, { useCallback, useMemo, useState } from "react";
import {
    Box,
    Button,
    ButtonGroup,
    createTheme,
    Grid,
    Icon,
    Table,
    TableBody,
    TableCell,
    TableRow,
    TextField,
    ThemeProvider,
    Tooltip,
} from "@mui/material";
import PropTypes from "prop-types";
import { activeRowColor, selectedAndActiveRowColor, selectedRowColor } from "../styles";
import { timeMsToDuration, unixTimeMsToLocalTime } from "../utils";
import { useAnalyticsService } from "../services/analytics";
import { TeamViewSettingsPanel } from "./TeamTable";
import { StarHalf, EmojiEvents, LooksOne, Check } from "@mui/icons-material";

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
                    paddingRight: "0px",
                    fontSize: "0px"
                }
            }]
        },
    },
});

const featuredRunMediaTypes = [
    { text: "reaction", mediaType: "reactionVideo" },
    { text: "camera", mediaType: "camera" },
    { text: "screen", mediaType: "screen" },
    { text: "record", mediaType: "record" },
    { text: "photo", mediaType: "photo" },
];

const isActive = (e) => {
    return e.advertisement !== undefined || e.tickerMessage !== undefined || e.featuredRun !== undefined;
};

const EventTagsIcons = ({ event: { tags } }) => {
    if (tags.includes("accepted-first-to-solve")){
        return <StarHalf/>;
    } else if (tags.includes("accepted-winner")) {
        return <LooksOne/>;
    } else if (tags.includes("accepted-gold-medal")) {
        return <EmojiEvents/>;
    } else if (tags.includes("accepted")) {
        return <Check/>;
    }
    return <Icon/>;
};
EventTagsIcons.propTypes = {
    event: PropTypes.shape({
        tags: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
};

function MessagesTable({ messages, selectedRowId, onRowClick }) {
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
                    {messages.map((event, rowId) =>
                        <TableRow key={rowId} sx={{ backgroundColor: rowBackground(event), cursor: "pointer" }}
                            onClick={() => onRowClick(event)}>
                            <TableCell type="icon"><EventTagsIcons event={event}/></TableCell>
                            <TableCell>{event.type === "commentary" ? event.message : ""}</TableCell>
                            <TableCell><Tooltip title={unixTimeMsToLocalTime(event.timeUnixMs)}>
                                <span>{timeMsToDuration(event.relativeTimeMs)}</span>
                            </Tooltip></TableCell>
                        </TableRow>)}
                </TableBody>
            </Table>
        </ThemeProvider>);
}

MessagesTable.propTypes = {
    messages: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.any.isRequired,
            type: PropTypes.string.isRequired,
            relativeTimeMs: PropTypes.number.isRequired,
            timeUnixMs: PropTypes.number.isRequired,
        }).isRequired).isRequired,
    selectedRowId: PropTypes.any,
    onRowClick: PropTypes.func.isRequired,
};

function Analytics() {
    const {
        messagesMap,
        messagesList,
        createAdvertisement,
        deleteAdvertisement,
        createTickerMessage,
        deleteTickerMessage,
        makeFeaturedRun,
        makeNotFeaturedRun,
    } = useAnalyticsService();

    const [selectedEventId, setSelectedEventId] = useState();
    const selectedEvent = useMemo(() => messagesMap[selectedEventId],
        [messagesMap, selectedEventId]);

    const [advertisementTtl, setAdvertisementTtl] = useState(30);
    const [tickerMsgTtl, setTickerMsgTtl] = useState(120);

    // const selectedEventMessage = selectedEvent?.type === "commentary" ? selectedEvent?.message : undefined;
    const selectedEventRun = selectedEvent?.runIds?.length === 1 ? selectedEvent?.runIds[0] : undefined;

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
                        onClick={() => createAdvertisement(selectedEvent?.id, advertisementTtl)}>advertisement</Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setAdvertisementTtl(e.target.value)}
                        value={advertisementTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "36.5px" } }}
                    />
                    <Button color="error" disabled={selectedEvent?.advertisement === undefined}
                        onClick={() => deleteAdvertisement(selectedEvent?.id)}>Hide</Button>
                </ButtonGroup>

                <ButtonGroup disabled={selectedEvent?.message === undefined}>
                    <Button color="primary" variant={"outlined"} startIcon={<ArrowForwardIcon/>}
                        disabled={selectedEvent?.tickerMessage !== undefined}
                        onClick={() => createTickerMessage(selectedEvent?.id, tickerMsgTtl)}>ticker</Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setTickerMsgTtl(e.target.value)}
                        value={tickerMsgTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "36.5px" } }}
                    />
                    <Button color="error" disabled={selectedEvent?.tickerMessage === undefined}
                        onClick={() => deleteTickerMessage(selectedEvent?.id)}>Hide</Button>
                </ButtonGroup>
            </Box>
            <Box sx={{ pl: 2, mt: 1 }}>
                    Featured run: <TeamViewSettingsPanel
                    isSomethingSelected={selectedEventRun !== undefined}
                    onShowTeam={(mediaTypes) => makeFeaturedRun(selectedEventId, mediaTypes[0])}
                    onHideTeam={() => makeNotFeaturedRun(selectedEventId)}
                    isPossibleToHide={selectedEvent?.featuredRun !== undefined}
                    mediaTypes={featuredRunMediaTypes}
                />
            </Box>
            <MessagesTable messages={messagesList} selectedRowId={selectedEvent?.id}
                onRowClick={({ id }) => setSelectedEventId(prevMsgId => id === prevMsgId ? undefined : id)}/>
        </Box>
    </Grid>
    );
}

export default Analytics;
