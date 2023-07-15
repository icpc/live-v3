import { Box, Button, ButtonGroup, Grid, Switch, TextField, Tooltip } from "@mui/material";
import React, { useEffect, useMemo, useState } from "react";
import { SelectTeamTable, TEAM_FIELD_STRUCTURE, TeamViewSettingsPanel } from "./TeamTable";
import PropTypes from "prop-types";
import { TeamViewService } from "../services/teamViewWidget";


export const TeamViewInstanceManager = ({
    instanceId,
    status,
    teams,
    selectedTeamId,
    onShow,
    onHide,
    mediaTypes,
}) =>
{
    const shownTeam = useMemo(
        () => teams.find(t => t.id === status.settings.teamId),
        [teams, status]);
    const [isStatusShown, setIsStatusShown] = useState(instanceId === null ? true : undefined);
    const [isAchievementShown, setIsAchievementShown] = useState(instanceId === null ? false : undefined);
    return (<Box>
        {instanceId && <Box><b>Instance {instanceId}</b></Box>}
        <Box>Shown team: {shownTeam?.name ?? "Auto"}</Box>
        <Box>Media type: {status.settings.mediaTypes?.join(", ")}</Box>
        <Box sx={{ pt: 1 }}>
            <TeamViewSettingsPanel
                mediaTypes={mediaTypes}
                canShow={selectedTeamId !== undefined}
                canHide={status.shown}
                onShowTeam={(mediaTypes) => onShow({
                    mediaTypes: mediaTypes,
                    teamId: selectedTeamId,
                    showTaskStatus: isStatusShown,
                    showAchievement: isAchievementShown })}
                onHideTeam={onHide} offerMultiple={true}
                isStatusShown={isStatusShown} setIsStatusShown={setIsStatusShown}
                isAchievementShown={isAchievementShown} setIsAchievementShown={setIsAchievementShown}
            />
        </Box>
    </Box>);
};
TeamViewInstanceManager.propTypes = {
    instanceId: PropTypes.any,
    status: PropTypes.shape({
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.shape({
            teamId: PropTypes.number,
            mediaTypes: PropTypes.arrayOf(PropTypes.string.isRequired),
        }).isRequired,
    }).isRequired,
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE),
    selectedTeamId: PropTypes.number,
    onShow: PropTypes.func,
    onHide: PropTypes.func,
    mediaTypes: TeamViewSettingsPanel.propTypes.mediaTypes,
};

const AUTOMODE_TEAM = {
    "id": null,
    "name": "Automode",
    "shortName": "Automode",
    "contestSystemId": null,
    "groups": [],
    "medias": {},
    "shown": false,
};

const isTeamSatisfiesSearch = (team, searchValue) => {
    if (searchValue === "" || team.id === null) {
        return true;
    }
    return (team.contestSystemId + " : " + team.shortName + " : " + team.name).toLowerCase().includes(searchValue);
};

export const TeamViewManager = ({ service, mediaTypes }) => {
    const [teams, setTeams] = useState([]);
    useEffect(() => {
        service.teams().then((ts) => setTeams([ AUTOMODE_TEAM, ...ts ]));
    }, [service]);

    const [selectedTeamId, setSelectedTeamId] = useState(undefined);

    const [status, setStatus] = useState({});
    const loadStatus = useMemo(() =>
        () => service.loadElements().then(setStatus),
    [service, setStatus]);
    useEffect(() => loadStatus(), []);
    useEffect(() => {
        service.addReloadDataHandler(loadStatus);
        return () => service.deleteReloadDataHandler(loadStatus);
    }, [service, loadStatus]);

    const teamsWithStatus = useMemo(
        () => teams.map(t => ({ ...t,
            shown: Object.values(status).some(s => s.shown && s.settings.teamId === t.id),
            selected: t.id === selectedTeamId,
        })),
        [teams, status, selectedTeamId]);

    const [searchValue, setSearchValue] = useState("");
    const filteredTeams = useMemo(() => {
        return teamsWithStatus.filter(t => isTeamSatisfiesSearch(t, searchValue));
    },[teamsWithStatus, searchValue]);
    console.log(filteredTeams);

    const [isMultipleMode, setIsMultipleMode] = useState(false);
    const selectTeam = (teamId) => setSelectedTeamId(teamId);

    const onShowInstance = (instanceId) => (settings) => {
        if (isMultipleMode) {
            service.editPreset(instanceId, settings);
        } else {
            service.showPresetWithSettings(instanceId, settings);
        }
    };

    const onHideInstance = (instanceId) => () => {
        service.hidePreset(instanceId);
    };

    return (<Grid>
        <Grid container rowSpacing={1} columnSpacing={{ xs: 1, sm: 2, md: 3 }} sx={{ width: 1 }}>
            {service.instances.filter(id => status[id] !== undefined).map(id =>
                <Grid item md={6} sm={12} key={id}>
                    <TeamViewInstanceManager
                        instanceId={id}
                        selectedTeamId={selectedTeamId}
                        status={status[id]}
                        teams={teamsWithStatus}
                        onShow={onShowInstance(id)}
                        onHide={onHideInstance(id)}
                        mediaTypes={mediaTypes}
                    />
                </Grid>
            )}
        </Grid>
        <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ py: 1 }}>
            <Tooltip
                title="When enabled any modifications to the team instances will be applied after you press Show all">
                <Box>
                    Multiple mode <Switch sx={{ mr: 2 }} onChange={(_, newV) => setIsMultipleMode(newV)}/>
                    <ButtonGroup>
                        <Button
                            sx={{ my: "2px" }}
                            variant={"contained"}
                            onClick={() => service.showAll()}>Show all</Button>
                        <Button
                            sx={{ my: "2px" }}
                            variant={"contained"}
                            color="error"
                            onClick={() => service.hideAll()}>Hide all</Button>
                    </ButtonGroup>
                </Box>
            </Tooltip>
            <Box><TextField
                onChange={(e) => setSearchValue(e.target.value.toLowerCase())}
                defaultValue={""}
                id="Search field"
                size="small"
                fullWidth
                margin="none"
                label="Search"
                variant="outlined"
                InputProps={{}}
            /></Box>
        </Box>
        <SelectTeamTable teams={filteredTeams} onClickHandler={selectTeam}/>
    </Grid>);
};
TeamViewManager.propTypes = {
    service: PropTypes.instanceOf(TeamViewService).isRequired,
    mediaTypes: PropTypes.arrayOf(PropTypes.string.isRequired),
};
