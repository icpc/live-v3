import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
    Container,
    Box,
    ToggleButtonGroup,
    ToggleButton,
    Grid,
    Paper,
    Stack,
    Button,
    ButtonGroup,
    Tooltip,
    Switch,
    TextField,
    InputAdornment,
    FormLabel,
    FormControl,
    FormControlLabel,
    ThemeProvider,
    createTheme,
} from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { TeamViewService, useTeamViewService } from "../services/teamViewWidget";
import SingleTeamViewIcon from "@mui/icons-material/WebAsset";
import PVPTeamViewIcon from "@mui/icons-material/Splitscreen";
import SplitTeamViewIcon from "@mui/icons-material/GridView";
import TopIcon from "@mui/icons-material/VerticalAlignTop";
import BottomIcon from "@mui/icons-material/VerticalAlignBottom";
import TopLeftIcon from "@mui/icons-material/NorthWest";
import TopRightIcon from "@mui/icons-material/NorthEast";
import BottomLeftIcon from "@mui/icons-material/SouthWest";
import BottomRightIcon from "@mui/icons-material/SouthEast";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import ArrowDropDown from "@mui/icons-material/ArrowDropDown";
import { SelectTeamTable, TEAM_FIELD_STRUCTURE, TeamViewSettingsPanel } from "./TeamTable";
import PropTypes from "prop-types";

const AUTOMODE_TEAM = {
    "id": null,
    "name": "Automode",
    "shortName": "Automode",
    "groups": [],
    "medias": {},
    "shown": false,
};

const useTeamviewService = (service, setStatus) => {
    const loadStatus = useMemo(() => {
        return () => service.loadElements().then(s => setStatus(st => ({ ...st, ...s })));
    }, [service, setStatus]);
    useEffect(() => loadStatus(), []);
    useEffect(() => {
        service.addReloadDataHandler(loadStatus);
        return () => service.deleteReloadDataHandler(loadStatus);
    }, [service, loadStatus]);
};

const isTeamSatisfiesSearch = (team, searchValue) => {
    if (searchValue === "" || team.id === null) {
        return true;
    }
    return (team.id + " : " + team.shortName + " : " + team.name).toLowerCase().includes(searchValue);
};

const useTeamsList = (rawTeams, status) => {
    const [selectedTeamId, setSelectedTeamId] = useState(undefined);
    const teamsWithStatus = useMemo(
        () => rawTeams.map(t => ({
            ...t,
            shown: Object.values(status).some(s => s.shown && s.settings.teamId === t.id),
            selected: t.id === selectedTeamId,
        })),
        [rawTeams, status, selectedTeamId]);

    const [searchValue, setSearchValue] = useState("");

    const filteredTeams = useMemo(() => {
        return teamsWithStatus.filter(t => isTeamSatisfiesSearch(t, searchValue));
    }, [teamsWithStatus, searchValue]);
    return { teams: filteredTeams, selectedTeamId, setSelectedTeamId, searchValue, setSearchValue };
};

const teamViewTheme = createTheme({
    components: {
        MuiPaper: {
            styleOverrides: {
                root: ({ theme }) => ({
                    padding: theme.spacing(2),
                    marginBottom: theme.spacing(1),
                }),
            },
        },
        MuiButton: {
            defaultProps: {
                size: "small",
            },
        },
        MuiButtonGroup: {
            defaultProps: {
                size: "small",
            },
        },
        MuiButtonBase: {
            styleOverrides: {
                root: {
                    margin: "0 !important",
                },
            },
        },
    },
});

const VariantSelect = ({ variant, setVariant }) => {
    return (
        <ToggleButtonGroup
            value={variant}
            color="primary"
            size="small"
            exclusive
            onChange={(_, v) => v && setVariant(v)}
        >
            <ToggleButton value={"single"}><SingleTeamViewIcon />Single</ToggleButton>
            <ToggleButton value={"pvp"}><PVPTeamViewIcon />PVP</ToggleButton>
            <ToggleButton value={"splitScreen"}><SplitTeamViewIcon />SplitScreen</ToggleButton>
        </ToggleButtonGroup>
    );
};

VariantSelect.propTypes = {
    variant: PropTypes.oneOf(["single", "pvp", "splitScreen"]).isRequired,
    setVariant: PropTypes.func.isRequired,
};

const InstanceStatus = ({ instanceId, Icon, status, teams, selectedInstance, onShow, onHide }) => {
    const iStatus = status[instanceId];
    const shownTeam = useMemo(
        () => teams.find(t => t.id === iStatus?.settings?.teamId),
        [teams, status]);
    const isShowButtonDisabled = !(selectedInstance === instanceId || selectedInstance === undefined);
    return (
        <Paper>
            <Stack sx={{ mb: 1 }} spacing={1} direction="row" flexWrap="wrap" alignItems={"center"}>
                {Icon && <Icon fontSize={"large"} color={iStatus?.shown ? "primary" : "disabled"} />}
                <ButtonGroup variant="contained" sx={{ m: 2 }}>
                    <Button color="primary" disabled={isShowButtonDisabled} onClick={onShow(instanceId)}>
                        {selectedInstance === instanceId ? "Selected" : (!iStatus?.shown ? "Show here" : "Replace here")}
                    </Button>
                    <Button color="error" disabled={!iStatus?.shown} onClick={onHide(instanceId)}>Hide</Button>
                </ButtonGroup>
            </Stack>
            <Box>Team: {shownTeam?.name ?? "Auto"}</Box>
            <Box>Media: {iStatus?.settings?.mediaTypes?.join(", ")}</Box>
        </Paper>
    );
};

InstanceStatus.propTypes = {
    instanceId: PropTypes.string,
    Icon: PropTypes.elementType.isRequired,
    status: PropTypes.object.isRequired,
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE).isRequired,
    selectedInstance: PropTypes.string,
    onShow: PropTypes.func.isRequired,
    onHide: PropTypes.func.isRequired,
};

const InstancesManager = ({ variant, ...props }) => {
    return (
        <>
            {variant === "single" && (
                <InstanceStatus instanceId={null} Icon={SingleTeamViewIcon} {...props} />
            )}
            {variant === "pvp" && (
                <>
                    <InstanceStatus instanceId={"PVP_TOP"} Icon={TopIcon} {...props} />
                    <InstanceStatus instanceId={"PVP_BOTTOM"} Icon={BottomIcon} {...props} />
                </>
            )}
            {variant === "splitScreen" && (
                <>
                    <Grid container columnSpacing={1}>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={"TOP_LEFT"} Icon={TopLeftIcon} {...props} />
                        </Grid>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={"TOP_RIGHT"} Icon={TopRightIcon} {...props} />
                        </Grid>
                    </Grid>
                    <Grid container columnSpacing={1}>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={"BOTTOM_LEFT"} Icon={BottomLeftIcon} {...props} />
                        </Grid>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={"BOTTOM_RIGHT"} Icon={BottomRightIcon} {...props} />
                        </Grid>
                    </Grid>
                </>
            )}
        </>
    );
};

InstancesManager.propTypes = {
    variant: PropTypes.string,
    instanceId: PropTypes.string,
    Icon: PropTypes.node,
    status: PropTypes.object.isRequired,
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE).isRequired,
    selectedInstance: PropTypes.string,
    onShow: PropTypes.func.isRequired,
    onHide: PropTypes.func.isRequired,
};

const TeamViewInstanceStatus = ({ instanceName, status, teams }) => {
    const shownTeam = useMemo(
        () => teams.find(t => t.id === status.settings.teamId),
        [teams, status]);
    return (
        <Box>
            <Box><b>Instance {instanceName ?? "SINGLE"}</b> {status.shown && "shown"}</Box>
            <Box>Team: {shownTeam?.name ?? "Auto"}</Box>
            <Box>Media: {status.settings.mediaTypes?.join(", ")}</Box>
        </Box>
    );
};

TeamViewInstanceStatus.propTypes = {
    instanceName: PropTypes.any,
    status: PropTypes.shape({
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.shape({
            teamId: PropTypes.number,
            mediaTypes: PropTypes.arrayOf(PropTypes.string.isRequired),
        }).isRequired,
    }).isRequired,
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE).isRequired,
};

const MultipleModeSwitch = ({ currentService, setIsMultipleMode }) => {
    return (
        <Tooltip
            sx={{ display: "flex", alignContent: "center" }}
            title="When enabled any modifications to the team instances will be applied after you press Show all"
        >
            <Box>
                <Box sx={{ p: 1 }}>
                    Mulitple mode
                </Box>
                <Switch onChange={(_, newV) => setIsMultipleMode(newV)} />
                <ButtonGroup>
                    <Button
                        variant="contained"
                        onClick={() => currentService?.showAll()}
                        startIcon={<VisibilityIcon />}
                    >
                        All
                    </Button>
                    <Button
                        variant="contained"
                        color="error"
                        onClick={() => currentService?.hideAll()}
                        startIcon={<VisibilityOffIcon />}
                    >
                        All
                    </Button>
                </ButtonGroup>
            </Box>
        </Tooltip>
    );
};

MultipleModeSwitch.propTypes = {
    currentService: PropTypes.instanceOf(TeamViewService).isRequired,
    setIsMultipleMode: PropTypes.func.isRequired,
};

const TeamViewManager = ({ singleService, pvpService, splitService }) => {
    const [status, setStatus] = useState({});
    useTeamviewService(singleService, setStatus);
    useTeamviewService(pvpService, setStatus);
    useTeamviewService(splitService, setStatus);

    const [variant, setVariant] = useState("single");
    useEffect(() => {
        if (Object.values(status).length === 7 && variant === undefined) {
            const shownInstance = Object.entries(status).find(([, i]) => i.shown);
            if (!shownInstance || shownInstance[0] === null) {
                setVariant("single");
            } else if (shownInstance[0].startsWith("PVP")) {
                setVariant("pvp");
            } else {
                setVariant("splitScreen");
            }
        }
    }, [status]);

    const currentService = useMemo(() => {
        if (variant === "splitScreen") {
            return splitService;
        } else if (variant === "pvp") {
            return pvpService;
        }
        return singleService;
    }, [variant, singleService, pvpService, splitService]);

    const [rawTeams, setRawTeams] = useState([]);
    const { teams, selectedTeamId, setSelectedTeamId, searchValue, setSearchValue } = useTeamsList(rawTeams, status);
    useEffect(() => {
        singleService.teams().then((ts) => setRawTeams([AUTOMODE_TEAM, ...ts]));
    }, [singleService]);

    const [isMultipleMode, setIsMultipleMode] = useState(false);

    const [selectedInstance, setSelectedInstance] = useState(undefined);
    const [mediaTypes1, setMediaTypes1] = useState(undefined);
    const [mediaTypes2, setMediaTypes2] = useState(undefined);
    const [statusShown, setStatusShown] = useState(true);
    const [achievementShown, setAchievementShown] = useState(false);

    const onShow = useCallback(() => {
        const settings = {
            mediaTypes: [mediaTypes1 && mediaTypes1[0], mediaTypes2 && mediaTypes2[0]].filter(i => i),
            teamId: selectedTeamId,
            showTaskStatus: statusShown,
            showAchievement: achievementShown,
        };
        if (isMultipleMode) {
            currentService.editPreset(selectedInstance, settings);
        } else {
            currentService.showPresetWithSettings(selectedInstance, settings);
        }
        setSelectedInstance(undefined);
        setSelectedTeamId(undefined);
    }, [selectedInstance, currentService, isMultipleMode, mediaTypes1,
        mediaTypes2, selectedTeamId, statusShown, achievementShown]);

    const onInstanceSelect = useCallback((instance) => () => {
        if (instance === selectedInstance) {
            setSelectedInstance(undefined);
        } else {
            setSelectedInstance(instance);
        }
    }, [selectedInstance]);

    const onInstanceHide = useCallback((instance) => () => {
        currentService.hidePreset(instance);
    }, [currentService]);

    const selectedTeamName = useMemo(() => {
        if (selectedTeamId === undefined) {
            return "";
        }
        return teams.find(team => team.id === selectedTeamId).name;
    }, [teams, selectedTeamId]);

    return (
        <Box>
            <Box sx={{ mb: 1 }} display="flex" flexWrap="wrap" justifyContent="space-between" alignItems="center">
                <VariantSelect variant={variant} setVariant={setVariant} />
                <MultipleModeSwitch currentService={currentService} setIsMultipleMode={setIsMultipleMode} />
            </Box>
            <InstancesManager
                variant={variant}
                status={status}
                teams={teams}
                selectedInstance={selectedInstance}
                onShow={onInstanceSelect}
                onHide={onInstanceHide}
            />

            {selectedInstance !== undefined &&
                <Paper>
                    {selectedTeamId === undefined && (
                        <Box>
                            <TextField
                                onChange={(e) => setSearchValue(e.target.value.toLowerCase())}
                                defaultValue={searchValue}
                                size="small"
                                margin="none"
                                label="Search"
                                variant="outlined"
                                fullWidth
                            />
                            <SelectTeamTable teams={teams} onClickHandler={setSelectedTeamId} />
                        </Box>
                    )}
                    {selectedTeamId !== undefined && (
                        <FormControl fullWidth sx={{ mb: 1 }}>
                            <FormLabel component="legend">Team name</FormLabel>
                            <TextField
                                defaultValue={selectedTeamName}
                                variant="standard"
                                fullWidth
                                InputProps={{
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <ArrowDropDown />
                                        </InputAdornment>
                                    ),
                                    onClick: () => setSelectedTeamId(undefined),
                                }}
                            />
                        </FormControl>
                    )}

                    {selectedTeamId !== undefined && (
                        <>
                            <FormControl fullWidth sx={{ mb: 1 }}>
                                <FormLabel component="legend">Main content</FormLabel>
                                <TeamViewSettingsPanel
                                    canShow={true}
                                    onShowTeam={ts => setMediaTypes1(ts)}
                                    showHideButton={false}
                                    selectedMediaTypes={mediaTypes1}
                                />
                            </FormControl>
                            <FormControl fullWidth sx={{ mb: 1 }}>
                                <FormLabel component="legend">Additional content</FormLabel>
                                <TeamViewSettingsPanel
                                    canShow={true}
                                    onShowTeam={ts => setMediaTypes2(ts)}
                                    showHideButton={false}
                                    selectedMediaTypes={mediaTypes2}
                                />
                            </FormControl>
                            <FormControl fullWidth sx={{ mb: 1 }}>
                                <FormLabel component="legend">Show status</FormLabel>
                                <FormControlLabel
                                    control={<Switch checked={statusShown} onChange={(_, v) => setStatusShown(v)}/>}
                                    label={"Display the name of the team, their current ranking, and details about their tasks"}
                                />
                            </FormControl>
                            <FormControl fullWidth sx={{ mb: 1 }}>
                                <FormLabel component="legend">Show achievements</FormLabel>
                                <FormControlLabel
                                    control={(
                                        <Switch
                                            checked={achievementShown}
                                            onChange={(_, v) => setAchievementShown(v)}
                                        />
                                    )}
                                    label={"Enable this switch to show contestant achievements"}
                                />
                                <Box>
                                </Box>
                            </FormControl>


                            <Button
                                color="primary"
                                variant="contained"
                                onClick={onShow}
                            >
                                Show
                            </Button>
                        </>
                    )}
                </Paper>
            }
        </Box>
    );
};

TeamViewManager.propTypes = {
    singleService: PropTypes.instanceOf(TeamViewService).isRequired,
    pvpService: PropTypes.instanceOf(TeamViewService).isRequired,
    splitService: PropTypes.instanceOf(TeamViewService).isRequired,
};


function TeamView() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = useTeamViewService("single", errorHandlerWithSnackbar(enqueueSnackbar));
    const pvpService = useTeamViewService("pvp", errorHandlerWithSnackbar(enqueueSnackbar));
    const splitService = useTeamViewService("splitScreen", errorHandlerWithSnackbar(enqueueSnackbar));

    return (
        <Container sx={{ pt: 2 }}>
            <ThemeProvider theme={teamViewTheme}>
                <TeamViewManager singleService={service} pvpService={pvpService} splitService={splitService} />
            </ThemeProvider>
        </Container>
    );
}

export default TeamView;
