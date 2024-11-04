import { useContestInfo } from "@/services/contestInfo";
import React, { useState } from "react";
import { Tab, Container, Box, Grid } from "@mui/material";
import TabContext from "@mui/lab/TabContext";
import TabList from "@mui/lab/TabList";
import TabPanel from "@mui/lab/TabPanel";
import { timeMsToDuration, unixTimeMsToLocalTime } from "@/utils";
import { ContestStatus, ContestInfo, GroupId, TeamMediaType, MediaType, TeamInfo, ProblemInfo, OrganizationInfo } from "@shared/api.ts";
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
                    status.holdTimeMs && unixTimeMsToLocalTime(status.holdTimeMs) || "??"
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

const ContestInfoContainer = ({ contestInfo } : BasicContainerProps) => {
    return <div><Grid container direction="row">
        <Grid item xs={6} md={4}>
            Name
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.name}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            Result type
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.resultType}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            Status
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && <ContestInfoStatus status={contestInfo.status}/>}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            Contest length
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.contestLengthMs && timeMsToDuration(contestInfo.contestLengthMs) || "??"}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            Freeze time
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.freezeTimeMs && timeMsToDuration(contestInfo.freezeTimeMs)}
            {!contestInfo || !contestInfo.freezeTimeMs && "??"}
        </Grid>
    </Grid></div>;
};

const ProblemTableColumns: GridColDef<ProblemInfo>[] = [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "letter",
        headerName: "Letter",
        flex: 1,
    },
    {
        field: "name",
        headerName: "Name",
        flex: 4,
    },
    {
        field: "color",
        headerName: "Color",
        renderCell: ({ value }) => (
            <>
                <Brightness1Icon sx={{ color: value }} fontSize="small"/> {value}
            </>
        ),
        flex: 2,
    },
    {
        field: "isHidden",
        headerName: "Hidden",
        valueFormatter: value => value ? "Hidden" : "",
    },
];

const ProblemContainer = ({ contestInfo } : BasicContainerProps) => {
    if (!contestInfo) {
        return undefined;
    }
    return <DataGrid
        rows={contestInfo.problems}
        columns={ProblemTableColumns}
        initialState={{
            pagination: { paginationModel: { pageSize: 100 } },
        }}
        pageSizeOptions={[100]}
        autoHeight
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
};

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

const TeamTableColumns = (setOpen: (v: boolean) => void, setCurrentTeamMedias: (m: { [key in TeamMediaType]: MediaType }) => void): GridColDef<TeamInfo>[] => [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "name",
        headerName: "Name",
        flex: 4,
    },
    {
        field: "shortName",
        headerName: "Short name",
        flex: 2,
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
        field: "isOutOfContest",
        headerName: "Is Out Of Contest"
    },
    {
        field: "organizationId",
        headerName: "Organization Id"
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
        field: "isHidden",
        headerName: "Hidden",
        valueFormatter: value => value ? "Hidden" : "",
        flex: 0.5
    },
];

type TeamContainerProps = {
    setOpen: (newValue: boolean) => void;
    setCurrentTeamMedias: (newMediaTypes : { [key in TeamMediaType]: MediaType }) => void;
} & BasicContainerProps;
const TeamContainer = ({ contestInfo, setOpen, setCurrentTeamMedias } : TeamContainerProps) => {
    if (!contestInfo) {
        return undefined;
    }
    return <DataGrid
        rows={contestInfo.teams}
        columns={TeamTableColumns(setOpen, setCurrentTeamMedias)}
        initialState={{
            pagination: { paginationModel: { pageSize: 100 } },
        }}
        pageSizeOptions={[100]}
        autoHeight
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
};

const OrganizationTableColumns: GridColDef<OrganizationInfo>[] = [
    {
        field: "id",
        headerName: "ID"
    },
    {
        field: "displayName",
        headerName: "Display name",
        flex: 2,
    },
    {
        field: "fullName",
        headerName: "Full name",
        flex: 4,
    },
    {
        field: "isHidden",
        headerName: "Hidden",
        valueFormatter: value => value ? "Hidden" : "",
        flex: 0.5
    },
];

const OrganizationContainer = ({ contestInfo } : BasicContainerProps) => {
    if (!contestInfo) {
        return undefined;
    }
    return <DataGrid
        rows={contestInfo.organizations}
        columns={OrganizationTableColumns}
        initialState={{
            pagination: { paginationModel: { pageSize: 100 } },
        }}
        pageSizeOptions={[100]}
        autoHeight
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
};

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
        <Container sx={{ pt: 2 }}>
            <SimpleDialog open={open} onClose={handleClose} medias={currentTeamMedias}/>
            <Box sx={{ width: "100%", typography: "body1" }}>
                <TabContext value={value}>
                    <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
                        <TabList onChange={handleChange} aria-label="lab API tabs example">
                            <Tab label="Contest" value="Contest" />
                            <Tab label="Problems" value="Problems" />
                            <Tab label="Teams" value="Teams" />
                            <Tab label="Organizations" value="Organizations" />
                        </TabList>
                    </Box>
                    <TabPanel value="Contest">
                        <ContestInfoContainer contestInfo={contestInfo}/>
                    </TabPanel>
                    <TabPanel value="Problems">
                        <ProblemContainer contestInfo={contestInfo}/>
                    </TabPanel>
                    <TabPanel value="Teams">
                        <TeamContainer contestInfo={contestInfo} setOpen={setOpen} setCurrentTeamMedias={setCurrentTeamMedias}/>
                    </TabPanel>
                    <TabPanel value="Organizations">
                        <OrganizationContainer contestInfo={contestInfo}/>
                    </TabPanel>
                </TabContext>
            </Box>
        </Container>
    );
};

export default ContestInfoPage;
