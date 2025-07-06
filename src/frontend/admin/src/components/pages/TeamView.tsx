import { useCallback, useEffect, useMemo, useState } from "react";
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
    ThemeProvider,
    createTheme,
} from "@mui/material";
import { SvgIconComponent } from "@mui/icons-material";
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
import { SelectTeamTable } from "../TeamTable.jsx";
import TeamMediaSwitcher, { DEFAULT_MEDIA_TYPES } from "../controls/TeamMediaSwitcher.js";
import ShowPresetButton from "../controls/ShowPresetButton.js";
import { TeamInfo, TeamMediaType, TeamViewPosition } from "@shared/api.ts";
import {
    CommonTeamViewInstancesState,
    TeamViewContentType,
    TeamViewWidgetService,
    useTeamViewWidgetService,
    useTeamViewWidgetUsageStats
} from "@/services/teamViewService.ts";

type TeamInfoWithStatus = TeamInfo & {
    shown?: boolean;
    selected?: boolean;
}

const AUTOMODE_TEAM: TeamInfoWithStatus = {
    id: null,
    name: "Automode",
    shortName: "Automode",
    hashTag: "",
    groups: [],
    organizationId: "",
    // TODO:
    medias: {
        [TeamMediaType.camera]: null,
        [TeamMediaType.screen]: null,
        [TeamMediaType.photo]: null,
        [TeamMediaType.reactionVideo]: null,
        [TeamMediaType.record]: null,
        [TeamMediaType.achievement]: null,
        [TeamMediaType.audio]: null,
        [TeamMediaType.backup]: null,
        [TeamMediaType.keylog]: null,
        [TeamMediaType.toolData]: null,
    },
    customFields: {},
    isHidden: false,
    isOutOfContest: false,
};

const isTeamSatisfiesSearch = (team: TeamInfo, searchValue: string) => {
    if (searchValue === "" || team.id === null) {
        return true;
    }
    return (team.id + " : " + team.shortName + " : " + team.name).toLowerCase().includes(searchValue);
};

// TODO: move to common :|
const useTeamsList = (rawTeams: TeamInfo[], status: CommonTeamViewInstancesState) => {
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
    const selectedTeam = useMemo(() => {
        if (selectedTeamId === undefined) {
            return undefined;
        }
        return rawTeams.find(team => team.id === selectedTeamId);
    }, [rawTeams, selectedTeamId]);
    return { teams: filteredTeams, selectedTeamId, setSelectedTeamId, selectedTeam, searchValue, setSearchValue };
};

const teamViewTheme = createTheme({
    components: {
        MuiPaper: {
            styleOverrides: {
                root: ({ theme }) => ({
                    padding: theme.spacing(1),
                    marginBottom: theme.spacing(1),
                }),
            },
        },
    },
});

type VariantSelectProps = {
    variant: TeamViewContentType;
    setVariant: (v: TeamViewContentType) => void;
}
const VariantSelect = ({ variant, setVariant }: VariantSelectProps) => {
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
            <ToggleButton value={"split"}><SplitTeamViewIcon />SplitScreen</ToggleButton>
        </ToggleButtonGroup>
    );
};

type InstanceStatusProps = {
    instanceId: TeamViewPosition;
    Icon: SvgIconComponent;
    status: CommonTeamViewInstancesState;
    teams: TeamInfoWithStatus[];
    selectedInstance?: TeamViewPosition;
    onShow: (instance: TeamViewPosition) => () => void;
    onHide: (instance: TeamViewPosition) => () => void;
};
const InstanceStatus = ({ instanceId, Icon, status, teams, selectedInstance, onShow, onHide }: InstanceStatusProps) => {
    const iStatus = status[instanceId]; // Why we can't do it outside?
    const shownTeam = useMemo(
        () => teams.find(t => t.id === iStatus?.settings?.teamId),
        [teams, iStatus]);
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
            <Box>Team: {shownTeam?.shortName ?? "Auto"}</Box>
            <Box>Media: {iStatus?.settings?.mediaTypes?.join(", ")}</Box>
        </Paper>
    );
};

type InstancesManagerProps = Omit<Omit<InstanceStatusProps, "instanceId">, "Icon"> & { variant: TeamViewContentType };
const InstancesManager = ({ variant, ...props }: InstancesManagerProps) => {
    return (
        <>
            {variant === "single" && (
                <InstanceStatus instanceId={TeamViewPosition.SINGLE} Icon={SingleTeamViewIcon} {...props} />
            )}
            {variant === "pvp" && (
                <>
                    <InstanceStatus instanceId={TeamViewPosition.PVP_TOP} Icon={TopIcon} {...props} />
                    <InstanceStatus instanceId={TeamViewPosition.PVP_BOTTOM} Icon={BottomIcon} {...props} />
                </>
            )}
            {variant === "split" && (
                <>
                    <Grid container columnSpacing={1}>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={TeamViewPosition.TOP_LEFT} Icon={TopLeftIcon} {...props} />
                        </Grid>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={TeamViewPosition.TOP_RIGHT} Icon={TopRightIcon} {...props} />
                        </Grid>
                    </Grid>
                    <Grid container columnSpacing={1}>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={TeamViewPosition.BOTTOM_LEFT} Icon={BottomLeftIcon} {...props} />
                        </Grid>
                        <Grid item md={6} sm={12}>
                            <InstanceStatus instanceId={TeamViewPosition.BOTTOM_RIGHT} Icon={BottomRightIcon} {...props} />
                        </Grid>
                    </Grid>
                </>
            )}
        </>
    );
};

type MultipleModeSwitchProps = {
    currentService: TeamViewWidgetService;
    setIsMultipleMode: (newMove: boolean) => void;
};
const MultipleModeSwitch = ({ currentService, setIsMultipleMode }: MultipleModeSwitchProps) => {
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

const TeamViewManager = () => {
    const [status, setStatus] = useState<CommonTeamViewInstancesState>({});
    const singleService = useTeamViewWidgetService("single", setStatus);
    const pvpService = useTeamViewWidgetService("pvp", setStatus);
    const splitService = useTeamViewWidgetService("split", setStatus);
    const usageStats = useTeamViewWidgetUsageStats(singleService);

    const [variant, setVariant] = useState<TeamViewContentType>(undefined);

    const currentService: TeamViewWidgetService = useMemo(() => {
        if (variant === "split") {
            return splitService;
        } else if (variant === "pvp") {
            return pvpService;
        }
        return singleService;
    }, [variant, singleService, pvpService, splitService]);

    const [rawTeams, setRawTeams] = useState<TeamInfoWithStatus[]>([]);
    const { teams, selectedTeamId, setSelectedTeamId, selectedTeam, searchValue, setSearchValue } = useTeamsList(rawTeams, status);
    useEffect(() => {
        singleService.teams().then((ts) => setRawTeams([AUTOMODE_TEAM, ...ts]));
    }, [singleService]);
    const [teamsAvailableMedias, teamsHasAchievement] = useMemo(() => {
        const medias = new Set();
        const hasAchievement = rawTeams.some(t => t.medias.achievement);
        rawTeams.forEach(t => Object.keys(t.medias).forEach(m => medias.add(m)));
        return [[...medias], hasAchievement];
    }, [rawTeams]);

    const [isMultipleMode, setIsMultipleMode] = useState(false);

    const [selectedInstance, setSelectedInstance] = useState(undefined);
    const [mediaType1, setMediaType1] = useState<TeamMediaType>(undefined);
    const [mediaType2, setMediaType2] = useState<TeamMediaType>(undefined);
    const [statusShown, setStatusShown] = useState(true);
    const [achievementShown, setAchievementShown] = useState(false);
    const [timeLineShown, setTimeLineShown] = useState(true);

    const [allowedMediaTypes, disableMediaTypes] = useMemo(() => [
        DEFAULT_MEDIA_TYPES.filter(m => m && (selectedTeam?.id ? selectedTeam.medias[m] : teamsAvailableMedias.includes(m))),
        DEFAULT_MEDIA_TYPES.filter(m => m && !(selectedTeam?.id ? selectedTeam.medias[m] : teamsAvailableMedias.includes(m)))
    ], [teamsAvailableMedias, selectedTeam]);


    useEffect(() => {
        if (Object.values(status).length === 7 && variant === undefined) {
            const shownInstance = Object.entries(status).find(([, i]) => i.shown);
            if (!shownInstance || shownInstance[0] === null) {
                setVariant("single");
            } else if (shownInstance[0].startsWith("PVP")) {
                setVariant("pvp");
            } else {
                setVariant("split");
            }

            if (mediaType1 === undefined && rawTeams.length > 0) {
                if (shownInstance && shownInstance[1].settings.mediaTypes.length > 0) {
                    setMediaType1(shownInstance[1].settings.mediaTypes[0]);
                } else {
                    setMediaType1(allowedMediaTypes.length > 0 ? allowedMediaTypes[0] : null);
                }
            }
            if (mediaType2 === undefined && rawTeams.length > 0) {
                if (shownInstance && shownInstance[1].settings.mediaTypes.length > 1) {
                    setMediaType2(shownInstance[1].settings.mediaTypes[1]);
                } else {
                    setMediaType2(allowedMediaTypes.length > 1 ? allowedMediaTypes[1] : null);
                }
            }
            if (rawTeams.some(t => t.medias.achievement)) {
                setAchievementShown(true);
            }
            setTimeLineShown(true);
        }
    }, [status, mediaType1, setMediaType1, mediaType2, setMediaType2, setAchievementShown, rawTeams, variant, allowedMediaTypes]);

    const onShow = useCallback(() => {
        const settings = {
            mediaTypes: [mediaType1, mediaType2].filter(i => i),
            teamId: selectedTeamId,
            showTaskStatus: statusShown,
            showAchievement: achievementShown && variant !== "split",
            showTimeLine: timeLineShown && variant === "single",
        };
        if (isMultipleMode) {
            currentService.setSettings(selectedInstance, settings);
        } else {
            currentService.showWithSettings(selectedInstance, settings);
        }
        setSelectedInstance(undefined);
        setSelectedTeamId(undefined);
    }, [selectedInstance, currentService, isMultipleMode, mediaType1, mediaType2,
        selectedTeamId, setSelectedTeamId, statusShown, achievementShown, variant, timeLineShown]);

    const onInstanceSelect = useCallback((instance: TeamViewPosition) => () => {
        if (instance === selectedInstance) {
            setSelectedInstance(undefined);
        } else {
            setSelectedInstance(instance);
        }
    }, [selectedInstance]);

    const onInstanceHide = useCallback((instance) => () => {
        currentService.hide(instance);
    }, [currentService]);

    return (
        <Box>
            <Box sx={{ mb: 1 }} display="flex" flexWrap="wrap" justifyContent="space-between" alignItems="center">
                <VariantSelect variant={variant ?? "single"} setVariant={setVariant} />
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
                            <SelectTeamTable
                                teams={teams}
                                onClickHandler={setSelectedTeamId}
                                usageStats={usageStats}
                            />
                        </Box>
                    )}
                    {selectedTeamId !== undefined && (
                        <FormControl fullWidth sx={{ mb: 1 }}>
                            <FormLabel component="legend">Team name</FormLabel>
                            <TextField
                                defaultValue={selectedTeam?.name}
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
                            <Grid container>
                                <Grid item xs={12} sm={4}>
                                    <FormLabel component="legend">Main content</FormLabel>
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TeamMediaSwitcher
                                        switchedMediaType={mediaType1}
                                        onSwitch={ts => setMediaType1(ts)}
                                        disabledMediaTypes={disableMediaTypes}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <FormLabel component="legend">Additional content</FormLabel>
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TeamMediaSwitcher
                                        switchedMediaType={mediaType2}
                                        onSwitch={ts => setMediaType2(ts)}
                                        disabledMediaTypes={[...disableMediaTypes, mediaType1].filter(t => t !== null)}
                                    />
                                </Grid>
                                <Grid item xs={10} sm={4}>
                                    <FormLabel component="legend">Name, ranking, submissions</FormLabel>
                                </Grid>
                                <Grid item xs={2} sm={8}>
                                    <ShowPresetButton
                                        checked={statusShown}
                                        onClick={(v) => setStatusShown(v)}
                                        sx={{ justifyContent: "flex-start" }}
                                    />
                                </Grid>
                                {variant !== "split" && (
                                    <>
                                        <Grid item xs={10} sm={4}>
                                            <FormLabel component="legend">Achievements</FormLabel>
                                        </Grid>
                                        <Grid item xs={2} sm={8}>
                                            <ShowPresetButton
                                                checked={achievementShown}
                                                onClick={(v) => setAchievementShown(v)}
                                                disabled={!(selectedTeam?.id ? selectedTeam.medias.achievement : teamsHasAchievement)}
                                                sx={{ justifyContent: "flex-start" }}
                                            />
                                        </Grid>
                                    </>
                                )}
                                {variant === "single" && (
                                    <>
                                        <Grid item xs={10} sm={4}>
                                            <FormLabel component="legend">TimeLine</FormLabel>
                                        </Grid>
                                        <Grid item xs={2} sm={8}>
                                            <ShowPresetButton
                                                checked={timeLineShown}
                                                onClick={(v) => setTimeLineShown(v)}
                                                sx={{ justifyContent: "flex-start" }}
                                            />
                                        </Grid>
                                    </>
                                )}
                            </Grid>
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

function TeamView() {
    return (
        <Container sx={{ pt: 2 }}>
            <ThemeProvider theme={teamViewTheme}>
                <TeamViewManager/>
            </ThemeProvider>
        </Container>
    );
}

export default TeamView;
