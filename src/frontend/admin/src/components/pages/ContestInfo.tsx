import { useContestInfo } from "@/services/contestInfo";
import React, { useState } from "react";
import { Tab, Container, Box, Grid } from "@mui/material";
import TabContext from "@mui/lab/TabContext";
import TabList from "@mui/lab/TabList";
import TabPanel from "@mui/lab/TabPanel";
import { timeMsToDuration, timeSecondsToDuration, unixTimeMsToLocalTime } from "@/utils";
import {
    ContestStatus,
    ContestInfo,
    GroupId,
    TeamMediaType,
    MediaType,
    TeamInfo,
    ProblemInfo,
    OrganizationInfo,
    GroupInfo,
    FtsMode,
    ProblemColorPolicy
} from "@shared/api.ts";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import Button from "@material-ui/core/Button";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import Dialog from "@material-ui/core/Dialog";
import Brightness1Icon from "@mui/icons-material/Brightness1";

const ContestInfoStatusTable = ({ rows }: { rows: [React.ReactNode, React.ReactNode][] }) => {
    return (
        <Grid container direction="column">
            {rows.map(([h, v], i) => (
                <Grid container direction="row" md={12} key={i}>
                    <Grid item xs={6} md={4}>
                        {h}
                    </Grid>
                    <Grid item xs={6} md={8}>
                        {v}
                    </Grid>
                </Grid>
            ))}
        </Grid>
    );
};

type ContestInfoStatusProps = { status: ContestStatus };

const ContestInfoStatus = ({ status } : ContestInfoStatusProps) => {
    if (!status) {
        return <div>??</div>;
    }
    switch (status.type) {
    case ContestStatus.Type.before:
        return (
            <ContestInfoStatusTable rows={[
                ["Status", "before"],
                [
                    "Scheduled start time",
                    status.scheduledStartAtUnixMs && unixTimeMsToLocalTime(status.scheduledStartAtUnixMs) || "??"
                ],
                [
                    "Hold time",
                    status.holdTimeMs && timeMsToDuration(status.holdTimeMs) || "??"
                ]
            ]}/>
        );
    case ContestStatus.Type.running:
        return (
            <ContestInfoStatusTable rows={[
                ["Status", "running"],
                [
                    "Start at",
                    status.startedAtUnixMs && unixTimeMsToLocalTime(status.startedAtUnixMs) || "??"
                ],
                [
                    "Frozen at",
                    status.frozenAtUnixMs && unixTimeMsToLocalTime(status.frozenAtUnixMs) || "-"
                ]
            ]}/>
        );
    case ContestStatus.Type.over:
        return <div>over</div>;
    case ContestStatus.Type.finalized:
        return <div>finalized</div>;
    }
};

type BasicContainerProps = {
    contestInfo: ContestInfo;
};

const InfoRowContainer = ({ name, value }) => {
    return <Grid container direction="row">
        <Grid item xs={6} md={4}>
            {name}
        </Grid>
        <Grid item xs={6} md={8}>
            {value}
        </Grid>
    </Grid>
}

const problemPolicyStr = (policy?: ProblemColorPolicy) => {
    switch (policy?.type) {
        case null: return "??";
        case ProblemColorPolicy.Type.afterStart: return `Replace with ${policy.colorBeforeStart || "null"} before start`;
        case ProblemColorPolicy.Type.whenSolved: return `Replace with ${policy.colorBeforeSolved || "null"} before problem is solved`;
        case ProblemColorPolicy.Type.always: return `Always show problem color`;
    }
}

const ContestInfoContainer = ({ contestInfo } : BasicContainerProps) => {
    return <div>
        <InfoRowContainer name="Name" value = {contestInfo?.name} />
        <InfoRowContainer name="Result type" value = {contestInfo?.resultType} />
        <InfoRowContainer name="Status" value = {<ContestInfoStatus status={contestInfo?.status}/>} />
        <InfoRowContainer name="Contest length" value = {timeMsToDuration(contestInfo?.contestLengthMs)} />
        <InfoRowContainer name="Freeze time" value = {timeMsToDuration(contestInfo?.freezeTimeMs)} />
        <InfoRowContainer name="Penalty per wrong attempt" value = {timeSecondsToDuration(contestInfo?.penaltyPerWrongAttemptSeconds)} />
        <InfoRowContainer name="Penalty rounding mode" value = {contestInfo?.penaltyRoundingMode} />
        <InfoRowContainer name="Emulation speed" value = {contestInfo?.emulationSpeed} />
        <InfoRowContainer name="Show team without submissions" value = {contestInfo?.showTeamsWithoutSubmissions ? "Yes" : "No"} />
        <InfoRowContainer name="Problem color policy" value = {problemPolicyStr(contestInfo?.problemColorPolicy)} />
        {contestInfo?.customFields && Object.entries(contestInfo.customFields).map(([key, value]) => (
            <InfoRowContainer name={"Custom:" + key} value={value} />
        ))}
    </div>;
};

const QueueSettingsContainer = ( { contestInfo } : BasicContainerProps) => {
    return <div>
        <InfoRowContainer name="Time in queue normal" value = {timeSecondsToDuration(contestInfo?.queueSettings?.waitTimeSeconds)} />
        <InfoRowContainer name="Time in queue FTS" value = {timeSecondsToDuration(contestInfo?.queueSettings?.firstToSolveWaitTimeSeconds)} />
        <InfoRowContainer name="Time in queue featured" value = {timeSecondsToDuration(contestInfo?.queueSettings?.featuredRunWaitTimeSeconds)} />
        <InfoRowContainer name="Time in queue in progress" value = {timeSecondsToDuration(contestInfo?.queueSettings?.inProgressRunWaitTimeSeconds)} />
        <InfoRowContainer name="Max queue size" value = {contestInfo?.queueSettings?.maxQueueSize} />
        <InfoRowContainer name="Max untested runs" value = {contestInfo?.queueSettings?.maxUntestedRun} />
    </div>;
}

const SimpleGrid = ( { rows, columns }) => {
    if (rows == undefined)
        return undefined
    return <DataGrid
        rows={rows}
        columns={columns}
        initialState={{
            pagination: { paginationModel: { pageSize: 100 } },
        }}
        pageSizeOptions={[100]}
        autoHeight
        autosizeOnMount
        autosizeOptions={{expand: true}}
        getRowHeight={() => "auto"}
        columnHeaderHeight={30}
        sx={{
            "& .MuiDataGrid-footerContainer": {
                minHeight: 30,
                maxHeight: 30,
            },
            "& .MuiDataGrid-cell:focus": {
                outline: "0",
            }
        }}
    />;

}

export interface SimpleDialogProps {
    open: boolean;
    onClose: () => void;
    medias: { [key in TeamMediaType]: MediaType };
}

function SimpleDialog({ onClose, open, medias }: SimpleDialogProps) {
    const handleClose = () => {
        onClose();
    };
    return (
        <Dialog
            fullWidth={true}
            maxWidth={"lg"}
            onClose={handleClose}
            open={open}
        >
            <List>
                {medias && Object.entries(medias).map(([key, media]) => (
                    <ListItem key={key}>
                        <Grid container direction="row">
                            <Grid item xs={4} md={2}>{key}</Grid>
                            <Grid item xs={4} md={3}>{media.type}</Grid>
                            <Grid item xs={4} md={7}>{media.url}</Grid>
                        </Grid>
                    </ListItem>
                ))}
            </List>
        </Dialog>
    );
}

const ProblemTableColumns: GridColDef<ProblemInfo>[] = [
    {
        field: "ordinal",
        headerName: "Order",
        type: "number"
    },
    {
        field: "id",
        headerName: "ID",
    },
    {
        field: "letter",
        headerName: "Letter",
    },
    {
        field: "name",
        headerName: "Name",
    },
    {
        field: "color",
        headerName: "Color",
        renderCell: ({ value }) => (
            value && <>
                <Brightness1Icon sx={{ color: value }} fontSize="small"/> {value}
            </>
        ),
    },
    {
        field: "isHidden",
        headerName: "Hidden",
        type: "boolean",
    },
    {
        field: "maxScore",
        headerName: "Max Score",
        type: "number"
    },
    {
        field: "scoreMergeMode",
        headerName: "Merge mode",
    },
    {
        field: "ftsMode",
        headerName: "FTS mode",
        valueGetter: (v: FtsMode) => {
            switch (v.type) {
                case FtsMode.Type.auto:
                    return "auto";
                case FtsMode.Type.custom:
                    return `custom(${v.runId})`;
                case FtsMode.Type.hidden:
                    return "hidden";
            }
        }
    }
];

const TeamTableColumns = (setOpen: (v: boolean) => void, setCurrentTeamMedias: (m: { [key in TeamMediaType]: MediaType }) => void): GridColDef<TeamInfo>[] => [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "name",
        headerName: "Full name",
    },
    {
        field: "shortName",
        headerName: "Display name",
    },
    {
        field: "groups",
        headerName: "Groups",
        valueGetter: (v: GroupId[]) => v.map(c => c).join(", "),
    },
    {
        field: "hashTag",
        headerName: "HashTag"
    },
    {
        field: "organizationId",
        headerName: "Organization"
    },
    {
        field: "medias",
        headerName: "Medias",
        renderCell: ({ value }) => {
            const handleClickOpen = () => {
                setOpen(true);
                setCurrentTeamMedias(value);
            };

            if (!value) {
                return <div/>;
            }
            return <div>
                <Button variant="outlined" color="primary" onClick={handleClickOpen}>
                    Medias
                </Button>
            </div>;
        },
    },
    {
        field: "color",
        headerName: "Color",
        renderCell: ({ value }) => (
            value && <>
                <Brightness1Icon sx={{ color: value }} fontSize="small"/> {value}
            </>
        ),
    },
    {
        field: "isOutOfContest",
        headerName: "Out Of Contest",
        type: "boolean",
    },
    {
        field: "isHidden",
        headerName: "Hidden",
        type: "boolean"
    },
];

const OrganizationTableColumns: GridColDef<OrganizationInfo>[] = [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "displayName",
        headerName: "Display name",
    },
    {
        field: "fullName",
        headerName: "Full name",
    }
];

const GroupTableColumns: GridColDef<GroupInfo>[] = [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "displayName",
        headerName: "Display name",
    },
    {
        field: "isOutOfContest",
        headerName: "Out Of Contest",
        type: "boolean",
    },
    {
        field: "isHidden",
        headerName: "Hidden",
        type: "boolean"
    },
];


const ContestInfoPage = () => {
    const contestInfo = useContestInfo();

    const [value, setValue] = useState(location.hash.substring(1) !== "" ? location.hash.substring(1) : "Contest");

    const handleChange = (_, newValue: string) => {
        setValue(newValue);
        location.hash = "#" + newValue;
    };

    const [open, setOpen] = useState(false);
    const [currentTeamMedias, setCurrentTeamMedias] = useState<{ [key in TeamMediaType]: MediaType }>();

    const handleClose = () => {
        setOpen(false);
    };

    return (
        <Container maxWidth={"xl"} sx={{ pt: 2 }}>
            <SimpleDialog open={open} onClose={handleClose} medias={currentTeamMedias}/>
            <Box sx={{ width: "100%", typography: "body1" }}>
                <TabContext value={value}>
                    <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
                        <TabList onChange={handleChange} aria-label="lab API tabs example">
                            <Tab label="Contest" value="Contest" />
                            <Tab label="Problems" value="Problems" />
                            <Tab label="Teams" value="Teams" />
                            <Tab label="Groups" value="Groups" />
                            <Tab label="Organizations" value="Organizations" />
                            <Tab label="Queue" value="Queue" />
                        </TabList>
                    </Box>
                    <TabPanel value="Contest">
                        <ContestInfoContainer contestInfo={contestInfo}/>
                    </TabPanel>
                    <TabPanel value="Problems">
                        <SimpleGrid rows={contestInfo?.problems} columns={ProblemTableColumns}/>
                    </TabPanel>
                    <TabPanel value="Teams">
                        <SimpleGrid rows={contestInfo?.teams} columns={TeamTableColumns(setOpen, setCurrentTeamMedias)}/>
                    </TabPanel>
                    <TabPanel value="Groups">
                        <SimpleGrid rows={contestInfo?.groups} columns={GroupTableColumns}/>
                    </TabPanel>
                    <TabPanel value="Organizations">
                        <SimpleGrid rows={contestInfo?.organizations} columns={OrganizationTableColumns}/>
                    </TabPanel>
                    <TabPanel value="Queue">
                        <QueueSettingsContainer contestInfo={contestInfo}/>
                    </TabPanel>
                </TabContext>
            </Box>
        </Container>
    );
};

export default ContestInfoPage;
