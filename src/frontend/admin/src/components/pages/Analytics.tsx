import { useCallback, useMemo, useRef, useState } from "react";
import {
    Box,
    Button,
    Checkbox,
    Grid,
    TextField,
    Tooltip,
} from "@mui/material";
import { ArrowForward as ArrowForwardIcon, StarHalf, EmojiEvents, LooksOne, Check } from "@mui/icons-material";
import { DataGrid, GridColDef, GridRenderCellParams, GridRowSelectionModel } from "@mui/x-data-grid";
import { activeRowColor } from "@/styles.js";
import { timeMsToDuration, unixTimeMsToLocalTime } from "@/utils";
import { FeaturedRunStatus, useAnalyticsService } from "@/services/analytics";
import {
    AnalyticsMessage,
    AnalyticsMessageComment,
    ProblemInfo,
    RunResult,
    TeamInfo,
    TeamMediaType,
    TeamViewPosition
} from "@shared/api.ts";
import TeamMediaSwitcher from "@/components/controls/TeamMediaSwitcher";
import ButtonGroup from "@/components/atoms/ButtonGroup";
import { CommonTeamViewInstancesState, useTeamViewWidgetService } from "@/services/teamViewService";

const featuredRunMediaTypes = [
    TeamMediaType.screen, TeamMediaType.camera, TeamMediaType.record, TeamMediaType.photo, TeamMediaType.reactionVideo
];

const EventTagsIcons = ({ tags }: { tags: string[]  }) => {
    if (tags.includes("first-to-solved")){
        return <StarHalf/>;
    } else if (tags.includes("accepted-winner")) {
        return <LooksOne/>;
    } else if (tags.includes("accepted-gold-medal")) {
        return <EmojiEvents/>;
    } else if (tags.includes("accepted")) {
        return <Check/>;
    }
    return undefined; //<Icon/>;
};

const buildMessagesTableColumns = (
    teams: { [id: string]: TeamInfo },
    problems: { [id: string]: ProblemInfo },
    selectedMessageId: string | undefined,
    selectedCommentId: string | undefined,
    onSelectComment: (messageId: string, commentId: string) => void,
): GridColDef<AnalyticsMessage>[] => [
    {
        field: "tags",
        headerName: "",
        width: 26,
        minWidth: 26,
        cellClassName: "AnalyticsTableTagsCell",
        valueGetter: (v: string[]) => v.join(" "),
        renderCell: ({ row: { tags } }: GridRenderCellParams<AnalyticsMessage, string[]>) => (
            <EventTagsIcons tags={tags} />
        )
    },
    {
        field: "comments",
        headerName: "Messages",
        flex: 3,
        sortable: false,
        valueGetter: (v: AnalyticsMessageComment[]) => v.map(c => c.message).join(" "),
        renderCell: ({ row: { id, comments, featuredRun } }: GridRenderCellParams<AnalyticsMessage, AnalyticsMessageComment[]>) => (
            <>
                {comments.map((m: AnalyticsMessageComment) => (
                    <Box key={m.id} sx={{ color: m.advertisement || m.tickerMessage || featuredRun ? activeRowColor : undefined }}>
                        <Checkbox
                            checked={m.id === selectedCommentId}
                            onClick={(e) => { e.stopPropagation(); onSelectComment(id, m.id); }}
                            sx={{ p: 0 }}
                        />
                        &nbsp;{m.message}
                    </Box>
                ))}
                {comments.length === 0 && <Checkbox checked={id === selectedMessageId} sx={{ p: 0 }} />}
            </>
        ),
        colSpan: (_, message) => message.comments.length > 0 ? 2 : 1,
    },
    {
        field: "teamId",
        headerName: "Team",
        flex: 2,
        valueGetter: (id: string) => {
            const team = teams[id];
            return team && (team.id + " - " + team.shortName + " - " + teams.hashTag) || "";
        },
        valueFormatter: (_, message) =>  teams[message.teamId]?.shortName ?? "",
    },
    {
        field: "problemId",
        headerName: "Problem",
        width: 70,
        valueGetter: (_, message) =>
            message.runInfo && (problems[message.runInfo.problemId]?.letter + " - " + problems[message.runInfo.problemId]?.name),
        valueFormatter: (_, message) =>  message.runInfo && problems[message.runInfo.problemId]?.letter || "",
    },
    {
        field: "runId",
        headerName: "Run",
        width: 70,
        valueGetter: (_, message) => message.runInfo?.id ?? "",
    },
    {
        field: "runInfoResult",
        headerName: "Result",
        width: 50,
        valueGetter: (_, message): string => {
            if (message.runInfo?.result?.type === RunResult.Type.ICPC) {
                return message.runInfo.result.verdict.shortName;
            } else if (message.runInfo?.result?.type === RunResult.Type.IOI) {
                return message.runInfo.result.scoreAfter.toString(0);
            }
            return "";
        },
    },
    {
        field: "relativeTimeMs",
        headerName: "Time",
        width: 70,
        renderCell: (params: GridRenderCellParams<AnalyticsMessage, number>) => (
            <Tooltip title={unixTimeMsToLocalTime(params.row.timeUnixMs)}>
                <span>{timeMsToDuration(params.row.relativeTimeMs)}</span>
            </Tooltip>
        ),
    },
];


type MessagesTableProps = {
    messages: AnalyticsMessage[];
    teams: { [id: string]: TeamInfo };
    problems: { [id: string]: ProblemInfo };
    selectedRowId: string;
    onSelectRow: (rowId: string | null) => void;
    selectedCommentId: string;
    onSelectComment: (messageId: string, commentId: string | null) => void;
};
function MessagesTable({
    messages,
    teams,
    problems,
    selectedRowId,
    onSelectRow,
    selectedCommentId,
    onSelectComment
}: MessagesTableProps) {
    const ref = useRef<HTMLTableElement>(null);
    const rowSelectionModel: GridRowSelectionModel = selectedRowId 
        ? { type: 'include', ids: new Set([selectedRowId]) }
        : { type: 'include', ids: new Set() };

    return (
        <DataGrid
            ref={ref}
            rows={messages}
            columns={buildMessagesTableColumns(teams, problems, selectedRowId, selectedCommentId, onSelectComment)}
            initialState={{
                pagination: { paginationModel: { pageSize: 100 } },
            }}
            pageSizeOptions={[10, 25, 50, 100]}
            autoHeight
            getRowHeight={() => "auto"}
            columnHeaderHeight={30}
            onRowSelectionModelChange={(newRowSelectionModel) => {
                const selectedId = newRowSelectionModel.ids.size > 0 
                    ? Array.from(newRowSelectionModel.ids)[0] as string 
                    : null;
                onSelectRow(selectedId);
            }}
            rowSelectionModel={rowSelectionModel}
            sx={{
                "& .MuiDataGrid-footerContainer": {
                    minHeight: 30,
                    maxHeight: 30,
                },
                "& .MuiDataGrid-cell:focus": { outline: "0" },
                "& .AnalyticsTableTagsCell": { paddingX: "2px" }
            }}
        />
    );
}


type FeaturedRunControlProps  = {
    selectedEvent: AnalyticsMessage;
    makeFeaturedRun: (eventId: string, mediaType: TeamMediaType) => void;
    featuredRunStatus?: FeaturedRunStatus;
    makeNotFeaturedRun: (eventId: string) => void;
}
const FeaturedRunControl = ({ selectedEvent, makeFeaturedRun, featuredRunStatus, makeNotFeaturedRun }: FeaturedRunControlProps) => {
    return (
        <>
            Featured run:&nbsp;
            <TeamMediaSwitcher
                mediaTypes={featuredRunMediaTypes}
                onSwitch={(mediaType) => makeFeaturedRun(selectedEvent?.id, mediaType)}
                disabled={selectedEvent?.runInfo === undefined}
            />

            {featuredRunStatus && (
                <Box sx={{
                    width: "fit-content",
                    display: "inline-flex",
                    alignItems: "baseline",
                    ml: 1,
                    border: "1px solid rgba(0, 0, 0, 0.12)",
                    pl: 1,
                    borderRadius: "4px",
                    verticalAlign: "baseline",
                }}>
                    <div>
                        Featured run {featuredRunStatus.runInfo?.id} ({featuredRunStatus.teamInfo?.shortName})
                    </div>
                    &nbsp;
                    <Button
                        size="small"
                        variant={"outlined"}
                        color="error"
                        onClick={() => makeNotFeaturedRun(featuredRunStatus.messageId)}
                    >
                        hide
                    </Button>
                </Box>
            )}
        </>
    );
};

type TeamViewControlProps  = {
    selectedEvent?: AnalyticsMessage;
};
const TeamViewControl = ({ selectedEvent }: TeamViewControlProps) => {
    const [status, setStatus] = useState<CommonTeamViewInstancesState>({});
    const teamViewService = useTeamViewWidgetService("single", setStatus);

    return (
        <>
            <ButtonGroup>
                <Button
                    color="primary"
                    variant={"outlined"}
                    startIcon={<ArrowForwardIcon/>}
                    disabled={selectedEvent?.teamId === undefined}
                    onClick={() => teamViewService.showWithSettings(TeamViewPosition.SINGLE, {
                        teamId: selectedEvent.teamId,
                        mediaTypes: [TeamMediaType.camera, TeamMediaType.screen],
                        showTaskStatus: true,
                        showAchievement: true,
                        showTimeLine: true,
                    })}
                >
                    TeamView
                </Button>
                <Button
                    color="error"
                    disabled={status[TeamViewPosition.SINGLE]?.shown !== true}
                    onClick={() => teamViewService.hide(TeamViewPosition.SINGLE)}
                >
                    Hide
                </Button>
            </ButtonGroup>
        </>
    );
};

function Analytics() {
    const {
        messagesMap,
        messagesList,
        teams,
        problems,
        createAdvertisement,
        deleteAdvertisement,
        createTickerMessage,
        deleteTickerMessage,
        makeFeaturedRun,
        makeNotFeaturedRun,
        featuredRunStatus,
    } = useAnalyticsService();

    const [selectedEventId, setSelectedEventId] = useState<string>(undefined);
    const selectedEvent = useMemo(() => messagesMap[selectedEventId],
        [messagesMap, selectedEventId]);
    const [selectedCommentId, setSelectedCommentId] = useState<string>(undefined);
    const selectedComment = useMemo(() => {
        return selectedEvent?.comments?.find(c => c.id === selectedCommentId);
    }, [selectedEvent, selectedCommentId]);
    const onSelectRow = useCallback((id: string) => {
        if (selectedEventId == id) {
            setSelectedEventId(undefined);
            setSelectedCommentId(undefined);
        } else {
            setSelectedEventId(id);
            if (messagesMap[id] && messagesMap[id].comments.length > 0) {
                setSelectedCommentId(messagesMap[id].comments[0].id);
            }
        }
    }, [selectedEventId, setSelectedEventId, messagesMap]);
    const onSelectComment = useCallback((messageId: string, commentId: string) => {
        if (selectedCommentId == commentId) {
            setSelectedCommentId(undefined);
        } else {
            setSelectedEventId(messageId);
            setSelectedCommentId(commentId);
        }
    }, [selectedCommentId, setSelectedCommentId, setSelectedEventId]);


    const [advertisementTtl, setAdvertisementTtl] = useState(30);
    const [tickerMsgTtl, setTickerMsgTtl] = useState(120);

    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column",
        pl: 2, pr: 2
    }}>
        <Box sx={{ width: { "lg": "75%", "md": "100%", "sm": "100%", "xs": "100%" } }}>
            <Box sx={{ mt: 2 }}>
                <ButtonGroup disabled={selectedComment === undefined}>
                    <Button
                        color="primary"
                        variant={"outlined"}
                        startIcon={<ArrowForwardIcon/>}
                        disabled={!selectedComment || selectedComment?.advertisement !== undefined}
                        onClick={() => createAdvertisement(selectedEvent?.id, selectedComment?.id, advertisementTtl)}
                    >
                        advertisement
                    </Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setAdvertisementTtl(Number.parseInt(e.target.value))}
                        value={advertisementTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "30px" } }}
                    />
                    <Button
                        color="error"
                        disabled={selectedComment?.advertisement === undefined}
                        onClick={() => deleteAdvertisement(selectedEvent?.id, selectedComment?.id)}
                    >
                        Hide
                    </Button>
                </ButtonGroup>&nbsp;
                <ButtonGroup disabled={selectedComment === undefined}>
                    <Button
                        color="primary"
                        variant={"outlined"}
                        startIcon={<ArrowForwardIcon/>}
                        disabled={!selectedComment || selectedComment?.tickerMessage !== undefined}
                        onClick={() => createTickerMessage(selectedEvent?.id, selectedComment?.id, tickerMsgTtl)}
                    >
                        ticker
                    </Button>
                    <TextField
                        sx={{ width: "84px" }}
                        onChange={e => setTickerMsgTtl(Number.parseInt(e.target.value))}
                        value={tickerMsgTtl}
                        size="small"
                        label="TTL"
                        variant="outlined"
                        InputProps={{ style: { height: "30px" } }}
                    />
                    <Button
                        color="error"
                        disabled={selectedComment?.tickerMessage === undefined}
                        onClick={() => deleteTickerMessage(selectedEvent?.id, selectedComment?.id)}
                    >
                        Hide
                    </Button>
                </ButtonGroup>
                &nbsp;
                <TeamViewControl selectedEvent={selectedEvent}/>
            </Box>
            <Box sx={{ mb: 1 }}>
                <FeaturedRunControl
                    selectedEvent={selectedEvent}
                    makeFeaturedRun={makeFeaturedRun}
                    featuredRunStatus={featuredRunStatus}
                    makeNotFeaturedRun={makeNotFeaturedRun}
                />
            </Box>

            <MessagesTable
                messages={messagesList}
                teams={teams}
                problems={problems}
                selectedRowId={selectedEventId}
                onSelectRow={onSelectRow}
                selectedCommentId={selectedComment?.id}
                onSelectComment={onSelectComment}
            />
        </Box>
    </Grid>
    );
}

export default Analytics;
