import { useContestInfo } from "@/services/contestInfo";
import {useState} from "react";
import { Tab, Container, Box, Grid } from "@mui/material";
import TabContext from '@mui/lab/TabContext';
import TabList from '@mui/lab/TabList';
import TabPanel from '@mui/lab/TabPanel';
import { timeMsToDuration, unixTimeMsToLocalTime } from "../utils"
import {ContestStatus, ContestInfo, GroupId, TeamMediaType, MediaType, TeamInfo} from "@shared/api.ts";
import {DataGrid, GridColDef} from "@mui/x-data-grid";
import Button from '@material-ui/core/Button';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import Dialog from '@material-ui/core/Dialog';

type ContestInfoStatusProps = { status: ContestStatus };
type ContestInfoContainerProps = {
    contestInfo: ContestInfo;
    setOpen: (boolean) => void;
    setCurrentTeamMedias: (newMediaTypes : { [key in TeamMediaType]: MediaType }) => void;
};
const ContestInfoStatus = ({ status } : ContestInfoStatusProps) => {
    if (!status) {
        return <div>??</div>;
    }
    switch (status.type) {
        case ContestStatus.Type.before:
            return <Grid container direction="column">
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        Status
                    </Grid>
                    <Grid item xs={6} md={8}>
                        before
                    </Grid>
                </Grid>
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        scheduled start time
                    </Grid>
                    <Grid item xs={6} md={8}>
                        {status.scheduledStartAtUnixMs && unixTimeMsToLocalTime(status.scheduledStartAtUnixMs)}
                        {!status.scheduledStartAtUnixMs && "??"}
                    </Grid>
                </Grid>
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        hold time
                    </Grid>
                    <Grid item xs={6} md={8}>
                        {status.holdTimeMs && unixTimeMsToLocalTime(status.holdTimeMs)}
                        {!status.holdTimeMs && "??"}
                    </Grid>
                </Grid>
            </Grid>;


        case ContestStatus.Type.running:
            return <Grid container direction="column">
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        Status
                    </Grid>
                    <Grid item xs={6} md={8}>
                        running
                    </Grid>
                </Grid>
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        started time
                    </Grid>
                    <Grid item xs={6} md={8}>
                        {status.startedAtUnixMs && unixTimeMsToLocalTime(status.startedAtUnixMs)}
                        {!status.startedAtUnixMs && "??"}
                    </Grid>
                </Grid>
                <Grid container direction="row" md={12}>
                    <Grid item xs={6} md={4}>
                        frozen time
                    </Grid>
                    <Grid item xs={6} md={8}>
                        {status.frozenAtUnixMs && unixTimeMsToLocalTime(status.frozenAtUnixMs)}
                        {!status.frozenAtUnixMs && "??"}
                    </Grid>
                </Grid>
            </Grid>;
        case ContestStatus.Type.over:
            return <div>over</div>
        case ContestStatus.Type.finalized:
            return <div>finalized</div>
        default:
            return <div>??</div>
    }

}

const ContestInfoContainer = ({contestInfo} : ContestInfoContainerProps) => {
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
            resultType
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.resultType}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            status
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && <ContestInfoStatus status={contestInfo.status}/>}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            contestLengthMs
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.contestLengthMs && timeMsToDuration(contestInfo.contestLengthMs)}
            {!contestInfo || !contestInfo.contestLengthMs && "??"}
        </Grid>
    </Grid>
    <Grid container direction="row">
        <Grid item xs={6} md={4}>
            freezeTimeMs
        </Grid>
        <Grid item xs={6} md={8}>
            {contestInfo && contestInfo.freezeTimeMs && timeMsToDuration(contestInfo.freezeTimeMs)}
            {!contestInfo || !contestInfo.freezeTimeMs && "??"}
        </Grid>
    </Grid></div>
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

    if (!medias) {
        return <div/>;
    }

    return (
        <Dialog fullWidth={true}
                maxWidth={"lg"}
                onClose={handleClose}
                open={open}
                >
            <List>
                {Object.entries(medias).map(([key, media]) => (
                    <ListItem>
                        <ListItemText primary={key} />
                        <ListItemText primary={media.type} />
                        <ListItemText primary={media.url} />
                    </ListItem>
                ))}
            </List>
        </Dialog>
    );
}
const TeamTableColumns = (setOpen, setCurrentTeamMedias): GridColDef<TeamInfo>[] => [
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
        field: "isHidden",
        headerName: "Is Hidden"
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
        renderCell: ({value}) => {
            const handleClickOpen = () => {
                setOpen(true);
                setCurrentTeamMedias(value)
            };

            if (!value) {
                return <div/>;
            }
            return <div>
                <Button variant="outlined" color="primary" onClick={handleClickOpen}>
                    Medias
                </Button>

            </div>
        },
    },
];

const TeamContainer = ({contestInfo, setOpen, setCurrentTeamMedias} : ContestInfoContainerProps) => {
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
    />
}

const OrganizationTableColumns = () => [
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
    }
];
const OrganizationContainer = ({contestInfo} : ContestInfoContainerProps) => {
    return <DataGrid
        rows={contestInfo.organizations}
        columns={OrganizationTableColumns()}
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
    />
}
function BackendLog() {
    const contestInfo = useContestInfo();

    const [value, setValue] = useState('Contest');

    const handleChange = (_, newValue: string) => {
        setValue(newValue);
    };

    const [open, setOpen] = useState(false);
    const [currentTeamMedias, setCurrentTeamMedias] = useState<{ [key in TeamMediaType]: MediaType }>();

    const handleClose = () => {
        setOpen(false);
    };

    return (
        <Container sx={{ pt: 2 }}>
            <Box sx={{ width: '100%', typography: 'body1' }}>
                <TabContext value={value}>
                    <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                        <TabList onChange={handleChange} aria-label="lab API tabs example">
                            <Tab label="Contest" value="Contest" />
                            <Tab label="Team" value="Team" />
                            <Tab label="Organization" value="Organization" />
                        </TabList>
                    </Box>
                    <TabPanel value="Contest">
                       <ContestInfoContainer contestInfo={contestInfo} setOpen={setOpen} setCurrentTeamMedias={undefined}/>
                    </TabPanel>
                    <TabPanel value="Team">
                        <TeamContainer contestInfo={contestInfo} setOpen={setOpen} setCurrentTeamMedias={setCurrentTeamMedias}/>
                        <SimpleDialog open={open} onClose={handleClose} medias={currentTeamMedias}/>
                    </TabPanel>
                    <TabPanel value="Organization">
                        <OrganizationContainer contestInfo={contestInfo} setOpen={undefined} setCurrentTeamMedias={undefined}/></TabPanel>
                </TabContext>
            </Box>
        </Container>
    );
}

export default BackendLog;