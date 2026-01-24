import { useContestInfo } from "@/services/contestInfo";
import React, { useState } from "react";
import {
    Tab,
    Container,
    Box,
    Grid,
    Button,
    List,
    ListItem,
    Dialog,
} from "@mui/material";
import TabContext from "@mui/lab/TabContext";
import TabList from "@mui/lab/TabList";
import TabPanel from "@mui/lab/TabPanel";
import {
    timeMsToDuration,
    timeSecondsToDuration,
    unixTimeMsToLocalTime,
} from "@/utils";
import {
    ContestStatus,
    ContestInfo,
    GroupId,
    MediaType,
    TeamInfo,
    ProblemInfo,
    OrganizationInfo,
    GroupInfo,
    FtsMode,
    ProblemColorPolicy,
    TeamId,
} from "@shared/api.ts";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import Brightness1Icon from "@mui/icons-material/Brightness1";

const ContestInfoStatusTable = ({
    rows,
}: {
    rows: [React.ReactNode, React.ReactNode][];
}) => {
    return (
        <Grid container direction="column">
            {rows.map(([h, v], i) => (
                <Grid container direction="row" size={{ md: 12 }} key={i}>
                    <Grid size={{ xs: 6, md: 4 }}>{h}</Grid>
                    <Grid size={{ xs: 6, md: 8 }}>{v}</Grid>
                </Grid>
            ))}
        </Grid>
    );
};

type ContestInfoStatusProps = { status: ContestStatus };

const ContestInfoStatus = ({ status }: ContestInfoStatusProps) => {
    if (!status) {
        return <div>??</div>;
    }
    switch (status.type) {
        case ContestStatus.Type.before:
            return (
                <ContestInfoStatusTable
                    rows={[
                        ["Status", "before"],
                        [
                            "Scheduled start time",
                            (status.scheduledStartAtUnixMs &&
                                unixTimeMsToLocalTime(
                                    status.scheduledStartAtUnixMs,
                                )) ||
                                "??",
                        ],
                        [
                            "Hold time",
                            (status.holdTimeMs &&
                                timeMsToDuration(status.holdTimeMs)) ||
                                "??",
                        ],
                    ]}
                />
            );
        case ContestStatus.Type.running:
            return (
                <ContestInfoStatusTable
                    rows={[
                        ["Status", "running"],
                        [
                            "Start at",
                            (status.startedAtUnixMs &&
                                unixTimeMsToLocalTime(
                                    status.startedAtUnixMs,
                                )) ||
                                "??",
                        ],
                        [
                            "Frozen at",
                            (status.frozenAtUnixMs &&
                                unixTimeMsToLocalTime(status.frozenAtUnixMs)) ||
                                "-",
                        ],
                    ]}
                />
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

const InfoRowContainer = ({
    name,
    value,
}: {
    key?: string;
    name: string;
    value: React.ReactNode;
}) => {
    return (
        <Grid container direction="row">
            <Grid size={{ xs: 6, md: 4 }}>{name}</Grid>
            <Grid size={{ xs: 6, md: 8 }}>{value}</Grid>
        </Grid>
    );
};

const problemPolicyStr = (policy?: ProblemColorPolicy) => {
    switch (policy?.type) {
        case null:
            return "??";
        case ProblemColorPolicy.Type.afterStart:
            return `Replace with ${policy.colorBeforeStart || "null"} before start`;
        case ProblemColorPolicy.Type.whenSolved:
            return `Replace with ${policy.colorBeforeSolved || "null"} before problem is solved`;
        case ProblemColorPolicy.Type.always:
            return `Always show problem color`;
    }
};

const ContestInfoContainer = ({ contestInfo }: BasicContainerProps) => {
    return (
        <div>
            <InfoRowContainer name="Name" value={contestInfo?.name} />
            <InfoRowContainer
                name="Result type"
                value={contestInfo?.resultType}
            />
            <InfoRowContainer
                name="Status"
                value={<ContestInfoStatus status={contestInfo?.status} />}
            />
            <InfoRowContainer
                name="Contest length"
                value={timeMsToDuration(contestInfo?.contestLengthMs)}
            />
            <InfoRowContainer
                name="Freeze time"
                value={timeMsToDuration(contestInfo?.freezeTimeMs)}
            />
            <InfoRowContainer
                name="Penalty per wrong attempt"
                value={timeSecondsToDuration(
                    contestInfo?.penaltyPerWrongAttemptSeconds,
                )}
            />
            <InfoRowContainer
                name="Penalty rounding mode"
                value={contestInfo?.penaltyRoundingMode}
            />
            <InfoRowContainer
                name="Emulation speed"
                value={contestInfo?.emulationSpeed}
            />
            <InfoRowContainer
                name="Show team without submissions"
                value={contestInfo?.showTeamsWithoutSubmissions ? "Yes" : "No"}
            />
            <InfoRowContainer
                name="Problem color policy"
                value={problemPolicyStr(contestInfo?.problemColorPolicy)}
            />
            {contestInfo?.customFields &&
                Object.entries(contestInfo.customFields).map(([key, value]) => (
                    <InfoRowContainer
                        key={key}
                        name={"Custom:" + key}
                        value={String(value)}
                    />
                ))}
        </div>
    );
};

const QueueSettingsContainer = ({ contestInfo }: BasicContainerProps) => {
    return (
        <div>
            <InfoRowContainer
                name="Time in queue normal"
                value={timeSecondsToDuration(
                    contestInfo?.queueSettings?.waitTimeSeconds,
                )}
            />
            <InfoRowContainer
                name="Time in queue FTS"
                value={timeSecondsToDuration(
                    contestInfo?.queueSettings?.firstToSolveWaitTimeSeconds,
                )}
            />
            <InfoRowContainer
                name="Time in queue featured"
                value={timeSecondsToDuration(
                    contestInfo?.queueSettings?.featuredRunWaitTimeSeconds,
                )}
            />
            <InfoRowContainer
                name="Time in queue in progress"
                value={timeSecondsToDuration(
                    contestInfo?.queueSettings?.inProgressRunWaitTimeSeconds,
                )}
            />
            <InfoRowContainer
                name="Max queue size"
                value={contestInfo?.queueSettings?.maxQueueSize}
            />
            <InfoRowContainer
                name="Max untested runs"
                value={contestInfo?.queueSettings?.maxUntestedRun}
            />
        </div>
    );
};

const orgsCustomField = (info: ContestInfo, name: string, def: string) => {
    const data = info.organizations
        .map((t) => ({
            id: t.id,
            name: t.displayName,
            value: t.customFields[name],
        }))
        .filter((t) => t.value != null && t.value != def);
    const columns = [
        { field: "id", headerName: "ID" },
        { field: "name", headerName: "Organization" },
        { field: "value", headerName: "Overridden value" },
    ];
    if (data.length == 0) return "";
    return <SimpleGrid rows={data} columns={columns} />;
};

const AwardTableColumns = (info: ContestInfo) => [
    {
        field: "id",
        headerName: "ID",
    },
    {
        field: "citation",
        headerName: "Citation",
    },
    {
        field: "maxRank",
        headerName: "Max rank",
    },
    {
        field: "minScore",
        headerName: "Min score",
    },
    {
        field: "tieBreakMode",
        headerName: "Tie-break mode",
    },
    {
        field: "organizationLimit",
        headerName: "Organization limit",
    },
    {
        field: "organizationLimitCustomField",
        headerName: "Organization limit overrides",
        renderCell: ({ value, row }) =>
            orgsCustomField(info, value, row.organizationLimit),
    },
    {
        field: "chainOrganizationLimit",
        headerName: "Chain organization limit",
    },
    {
        field: "chainOrganizationLimitCustomField",
        headerName: "Chain organization limit overrides",
        renderCell: ({ value, row }) =>
            orgsCustomField(info, value, row.chainOrganizationLimit),
    },
    {
        field: "manualTeamIds",
        headerName: "Manual teams",
        valueGetter: (v: TeamId[]) => v.map((c) => c).join(", "),
    },
];

const AwardsContainer = ({ contestInfo }: BasicContainerProps) => {
    return (
        <div>
            {(contestInfo?.awardsSettings || [])?.map((chain, index) => (
                <Box
                    key={index}
                    sx={{ borderBottom: 1, borderColor: "divider" }}
                >
                    <InfoRowContainer
                        name={"Groups"}
                        value={
                            chain.groups?.length == 0
                                ? "all"
                                : chain.groups.join(", ")
                        }
                    />
                    <InfoRowContainer
                        name={"Excluded groups"}
                        value={
                            chain.excludedGroups?.length == 0
                                ? "none"
                                : chain.groups.join(", ")
                        }
                    />
                    <InfoRowContainer
                        name={"Organization Limit"}
                        value={
                            chain.organizationLimit === null
                                ? "unlimited"
                                : chain.organizationLimit
                        }
                    />
                    <InfoRowContainer
                        name={"Overridden organization limits"}
                        value={orgsCustomField(
                            contestInfo,
                            chain.organizationLimitCustomField,
                            chain.organizationLimit?.toString(),
                        )}
                    />
                    <SimpleGrid
                        rows={chain.awards}
                        columns={AwardTableColumns(contestInfo)}
                    />
                </Box>
            ))}
        </div>
    );
};

const SimpleGrid = ({ rows, columns }) => {
    if (rows == undefined) return undefined;
    return (
        <DataGrid
            rows={rows}
            columns={columns}
            initialState={{
                pagination: { paginationModel: { pageSize: 100 } },
            }}
            pageSizeOptions={[100]}
            autoHeight
            autosizeOnMount
            autosizeOptions={{ expand: true }}
            getRowHeight={() => "auto"}
            columnHeaderHeight={30}
            sx={{
                "& .MuiDataGrid-footerContainer": {
                    minHeight: 30,
                    maxHeight: 30,
                },
                "& .MuiDataGrid-cell:focus": {
                    outline: "0",
                },
            }}
        />
    );
};

export interface SimpleDialogProps {
    open: boolean;
    onClose: () => void;
    medias: { [key in string]: MediaType[] };
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
                {medias &&
                    Object.entries(medias).flatMap(([key, medias]) => {
                        return medias.map((media, idx) => (
                            <ListItem key={`${key}-${idx}`}>
                                <Grid container direction="row">
                                    <Grid size={{ xs: 4, md: 2 }}>
                                        {key}
                                        {medias.length > 1
                                            ? ` [${idx + 1}]`
                                            : ""}
                                    </Grid>
                                    <Grid size={{ xs: 4, md: 3 }}>
                                        {media?.type ?? "-"}
                                    </Grid>
                                    <Grid size={{ xs: 4, md: 7 }}>
                                        {media?.url ?? "-"}
                                    </Grid>
                                </Grid>
                            </ListItem>
                        ));
                    })}
            </List>
        </Dialog>
    );
}

const ProblemTableColumns: GridColDef<ProblemInfo>[] = [
    {
        field: "ordinal",
        headerName: "Order",
        type: "number",
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
        renderCell: ({ value }) =>
            value && (
                <>
                    <Brightness1Icon sx={{ color: value }} fontSize="small" />{" "}
                    {value}
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
        type: "number",
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
        },
    },
];

const TeamTableColumns = (
    setOpen: (v: boolean) => void,
    setCurrentTeamMedias: (m: { string: MediaType[] }) => void,
): GridColDef<TeamInfo>[] => [
    {
        field: "id",
        headerName: "ID",
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
        valueGetter: (v: GroupId[]) => v.map((c) => c).join(", "),
    },
    {
        field: "hashTag",
        headerName: "HashTag",
    },
    {
        field: "organizationId",
        headerName: "Organization",
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
                return <div />;
            }
            return (
                <div>
                    <Button
                        variant="outlined"
                        color="primary"
                        onClick={handleClickOpen}
                    >
                        Medias
                    </Button>
                </div>
            );
        },
    },
    {
        field: "color",
        headerName: "Color",
        renderCell: ({ value }) =>
            value && (
                <>
                    <Brightness1Icon sx={{ color: value }} fontSize="small" />{" "}
                    {value}
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
        type: "boolean",
    },
];

const OrganizationTableColumns = (
    setOpen: (v: boolean) => void,
    setCurrentTeamMedias: (m: { [key in string]: MediaType[] }) => void,
): GridColDef<OrganizationInfo>[] => [
    {
        field: "id",
        headerName: "ID",
    },
    {
        field: "displayName",
        headerName: "Display name",
    },
    {
        field: "fullName",
        headerName: "Full name",
    },
    {
        field: "logo",
        headerName: "Logo",
        renderCell: ({ value }) => {
            const handleClickOpen = () => {
                setOpen(true);
                setCurrentTeamMedias({ logos: value });
            };

            if (!value) {
                return <div />;
            }
            return (
                <div>
                    <Button
                        variant="outlined"
                        color="primary"
                        onClick={handleClickOpen}
                    >
                        Logos
                    </Button>
                </div>
            );
        },
    },
];

const GroupTableColumns: GridColDef<GroupInfo>[] = [
    {
        field: "id",
        headerName: "ID",
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
        type: "boolean",
    },
];

const ContestInfoPage = () => {
    const contestInfo = useContestInfo();

    const [value, setValue] = useState(
        location.hash.substring(1) !== ""
            ? location.hash.substring(1)
            : "Contest",
    );

    const handleChange = (_, newValue: string) => {
        setValue(newValue);
        location.hash = "#" + newValue;
    };

    const [open, setOpen] = useState(false);
    const [currentTeamMedias, setCurrentTeamMedias] =
        useState<{ [key in string]: MediaType[] }>();

    const handleClose = () => {
        setOpen(false);
    };

    return (
        <Container maxWidth={"xl"} sx={{ pt: 2 }}>
            <SimpleDialog
                open={open}
                onClose={handleClose}
                medias={currentTeamMedias}
            />
            <Box sx={{ width: "100%", typography: "body1" }}>
                <TabContext value={value}>
                    <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
                        <TabList
                            onChange={handleChange}
                            aria-label="lab API tabs example"
                        >
                            <Tab label="Contest" value="Contest" />
                            <Tab label="Problems" value="Problems" />
                            <Tab label="Teams" value="Teams" />
                            <Tab label="Groups" value="Groups" />
                            <Tab label="Organizations" value="Organizations" />
                            <Tab label="Queue" value="Queue" />
                            <Tab label="Awards" value="Awards" />
                        </TabList>
                    </Box>
                    <TabPanel value="Contest">
                        <ContestInfoContainer contestInfo={contestInfo} />
                    </TabPanel>
                    <TabPanel value="Problems">
                        <SimpleGrid
                            rows={contestInfo?.problems}
                            columns={ProblemTableColumns}
                        />
                    </TabPanel>
                    <TabPanel value="Teams">
                        <SimpleGrid
                            rows={contestInfo?.teams}
                            columns={TeamTableColumns(
                                setOpen,
                                setCurrentTeamMedias,
                            )}
                        />
                    </TabPanel>
                    <TabPanel value="Groups">
                        <SimpleGrid
                            rows={contestInfo?.groups}
                            columns={GroupTableColumns}
                        />
                    </TabPanel>
                    <TabPanel value="Organizations">
                        <SimpleGrid
                            rows={contestInfo?.organizations}
                            columns={OrganizationTableColumns(
                                setOpen,
                                setCurrentTeamMedias,
                            )}
                        />
                    </TabPanel>
                    <TabPanel value="Queue">
                        <QueueSettingsContainer contestInfo={contestInfo} />
                    </TabPanel>
                    <TabPanel value="Awards">
                        <AwardsContainer contestInfo={contestInfo} />
                    </TabPanel>
                </TabContext>
            </Box>
        </Container>
    );
};

export default ContestInfoPage;
