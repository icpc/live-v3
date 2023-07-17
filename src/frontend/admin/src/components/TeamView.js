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
    styled,
    InputAdornment,
    FormLabel,
} from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { useTeamViewService } from "../services/teamViewWidget";
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
    "contestSystemId": null,
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
    return (team.contestSystemId + " : " + team.shortName + " : " + team.name).toLowerCase().includes(searchValue);
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

const TEAMVIEW_INSTANCES = [
    { id: null, title: "single", icon: null },
    { id: "PVP_TOP", title: "PVP", icon: <TopIcon /> },
    { id: "PVP_BOTTOM", title: "PVP", icon: <BottomIcon /> },
    { id: "TOP_LEFT", title: "SplitScreen", icon: <TopLeftIcon /> },
    { id: "TOP_RIGHT", title: "SplitScreen", icon: <TopRightIcon /> },
    { id: "BOTTOM_LEFT", title: "SplitScreen", icon: <BottomLeftIcon /> },
    { id: "BOTTOM_RIGHT", title: "SplitScreen", icon: <BottomRightIcon /> },
];

const VariantSelect = ({ variant, setVariant }) => {
    return (
        <ToggleButtonGroup
            value={variant}
            color="primary"
            exclusive
            onChange={(_, v) => setVariant(v)}
        >
            <ToggleButton value={"single"}><SingleTeamViewIcon />Single</ToggleButton>
            <ToggleButton value={"pvp"}><PVPTeamViewIcon />PVP</ToggleButton>
            <ToggleButton value={"splitScreen"}><SplitTeamViewIcon />SplitScreen</ToggleButton>
        </ToggleButtonGroup>
    );
};

const InstanceStatusCard = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(2),
    marginBottom: theme.spacing(1),
}));

const InstanceStatus = ({ instanceId, Icon, status, teams, canShow, isSelected, onShow, onHide }) => {
    const iStatus = status[instanceId];
    const shownTeam = useMemo(
        () => teams.find(t => t.id === iStatus?.settings?.teamId),
        [teams, status]);
    return (
        <InstanceStatusCard>
            <Stack sx={{ mb: 1 }} spacing={1} direction="row" flexWrap="wrap" alignItems={"center"}>
                {Icon && <Icon fontSize={"large"} color={iStatus?.shown ? "primary" : "disabled"} />}
                <ButtonGroup variant="contained" sx={{ m: 2 }}>
                    <Button color="primary" disabled={!canShow(instanceId)} onClick={onShow(instanceId)}>
                        {isSelected(instanceId) ? "Selected" : (!iStatus?.shown ? "Show here" : "Replace here")}
                    </Button>
                    <Button color="error" disabled={!iStatus?.shown} onClick={onHide(instanceId)}>Hide</Button>
                </ButtonGroup>
            </Stack>
            <Box>Team: {shownTeam?.name ?? "Auto"}</Box>
            <Box>Media: {iStatus?.settings?.mediaTypes?.join(", ")}</Box>
        </InstanceStatusCard>
    );
};

InstanceStatus.propTypes = {
    instanceId: PropTypes.string,
    Icon: PropTypes.elementType.isRequired,
    status: PropTypes.object.isRequired,
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE).isRequired,
    canShow: PropTypes.func.isRequired,
    isSelected: PropTypes.func.isRequired,
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
    canShow: PropTypes.func.isRequired,
    isSelected: PropTypes.func.isRequired,
    onShow: PropTypes.func.isRequired,
    onHide: PropTypes.func.isRequired,
};

const TeamViewInstanceStatus = ({ instanceName, status, teams }) => {
    const shownTeam = useMemo(
        () => teams.find(t => t.id === status.settings.teamId),
        [teams, status]);
    return (<Box>
        <Box><b>Instance {instanceName ?? "SINGLE"}</b> {status.shown && "shown"}</Box>
        <Box>Team: {shownTeam?.name ?? "Auto"}</Box>
        <Box>Media: {status.settings.mediaTypes?.join(", ")}</Box>
    </Box>);
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
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE),
};

const MultipleModeSwitch = ({ currentService, setIsMultipleMode }) => {
    return (
        <Tooltip
            sx={{ display: "flex", alignContent: "center", mr: 1 }}
            title="When enabled any modifications to the team instances will be applied after you press Show all"
        >
            <Box>
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


const TeamViewManager = ({ service, pvpService, splitService }) => {
    const [status, setStatus] = useState({});
    useTeamviewService(service, setStatus);
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
        return service;
    }, [variant, service, pvpService, splitService]);

    const [rawTeams, setRawTeams] = useState([]);

    useEffect(() => {
        service.teams().then((ts) => setRawTeams([AUTOMODE_TEAM, ...ts]));
    }, [service]);

    const { teams, selectedTeamId, setSelectedTeamId, searchValue, setSearchValue } = useTeamsList(rawTeams, status);

    const [selectedInstance, setSelectedInstance] = useState(undefined);

    const [isMultipleMode, setIsMultipleMode] = useState(false);

    const [mediaTypes, setMediaTypes] = useState();
    const [isStatusShown, setIsStatusShown] = useState(true);
    const [isAchievementShown, setIsAchievementShown] = useState(false);

    const onShow = useCallback((instance) => () => {
        const settings = {
            mediaTypes: mediaTypes,
            teamId: selectedTeamId,
            showTaskStatus: isStatusShown,
            showAchievement: isAchievementShown,
        };
        if (isMultipleMode) {
            currentService.editPreset(instance, settings);
        } else {
            currentService.showPresetWithSettings(instance, settings);
        }
        setSelectedInstance(undefined);
        setSelectedTeamId(undefined);
    }, [currentService, isMultipleMode, mediaTypes, selectedTeamId, isStatusShown, isAchievementShown]);

    const onInstanceShow = useCallback((instance) => () => {
        if (instance === selectedInstance) {
            setSelectedInstance(undefined);
        } else {
            setSelectedInstance(instance);
        }
    }, [selectedInstance]);

    const onInstanceHide = useCallback((instance) => () => {
        currentService.hidePreset(instance);
    }, [currentService]);

    const isInstanceSelected = useMemo(() => (instance) => {
        return selectedInstance === instance;
    }, [selectedInstance]);

    const canInstanceShow = useMemo(() => (instance) => {
        return selectedInstance === undefined || selectedInstance === instance;
    }, [selectedInstance]);

    const selectedTeamName = useMemo(() => {
        if (selectedTeamId === undefined) {
            return "";
        }
        return teams.find(team => team.id == selectedTeamId).name;
    }, [teams, selectedTeamId]);

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Box display="flex" justifyContent="space-between" alignItems="center">
                    <VariantSelect variant={variant} setVariant={setVariant} />
                    <MultipleModeSwitch currentService={currentService} setIsMultipleMode={setIsMultipleMode} />
                </Box>
                <InstancesManager
                    variant={variant}
                    status={status}
                    teams={teams}
                    canShow={canInstanceShow}
                    isSelected={isInstanceSelected}
                    onShow={onInstanceShow}
                    onHide={onInstanceHide}
                />
            </Box>

            {selectedInstance !== undefined &&
                <Box>
                    <Box sx={{ mb: 3 }}>
                        {selectedTeamId === undefined &&
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
                            </Box>}
                        {selectedTeamId !== undefined &&
                            <TextField
                                defaultValue={selectedTeamName}
                                label="Team name"
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
                            />}
                    </Box>

                    {selectedTeamId !== undefined &&
                        <Box sx={{ mb: 3 }}>
                            <Box display="flex" justifyContent="space-between" alignItems="center">
                                <Box>
                                    <Box sx={{ mb: 3 }}>
                                        <FormLabel component="legend">Main content</FormLabel>
                                        <TeamViewSettingsPanel
                                            canShow={true}
                                            onShowTeam={ts => setMediaTypes(ts)}
                                            showHideButton={false}
                                            offerMultiple={false}
                                            isStatusShown={variant === "single" ? isStatusShown : undefined}
                                            setIsStatusShown={setIsStatusShown}
                                            isAchievementShown={variant === "single" ? isAchievementShown : undefined}
                                            setIsAchievementShown={setIsAchievementShown}
                                            selectedMediaTypes={mediaTypes}
                                        />
                                    </Box>
                                    <Box sx={{ mb: 3 }}>
                                        <FormLabel component="legend">Additional content</FormLabel>
                                        <TeamViewSettingsPanel
                                            canShow={true}
                                            onShowTeam={ts => setMediaTypes(ts)}
                                            showHideButton={false}
                                            offerMultiple={false}
                                            isStatusShown={variant === "single" ? isStatusShown : undefined}
                                            setIsStatusShown={setIsStatusShown}
                                            isAchievementShown={variant === "single" ? isAchievementShown : undefined}
                                            setIsAchievementShown={setIsAchievementShown}
                                            selectedMediaTypes={mediaTypes}
                                        />
                                    </Box>
                                </Box>

                                <Button color="primary"
                                    variant="contained"
                                    onClick={onShow(selectedInstance)}>
                                    Show
                                </Button>
                            </Box>
                        </Box>
                    }
                </Box>
            }
        </Box>
    );
};

function TeamView() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = useTeamViewService("singe", errorHandlerWithSnackbar(enqueueSnackbar));
    const pvpService = useTeamViewService("pvp", errorHandlerWithSnackbar(enqueueSnackbar));
    const splitService = useTeamViewService("splitScreen", errorHandlerWithSnackbar(enqueueSnackbar));

    return (
        <Container sx={{ pt: 2 }}>
            <TeamViewManager service={service} pvpService={pvpService} splitService={splitService} />
        </Container>
    );
}

export default TeamView;
