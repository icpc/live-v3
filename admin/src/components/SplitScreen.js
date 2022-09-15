import { Grid, Switch } from "@mui/material";
import React, { useEffect, useState } from "react";
import { useSnackbar } from "notistack";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import Box from "@mui/material/Box";
import { SelectTeamTable, TeamViewSettingsPanel } from "./TeamTable";
import PropTypes from "prop-types";
import { BASE_URL_BACKEND } from "../config";
import { createApiGet, createApiPost } from "../utils";


function SplitScreenInstance({ instanceId, selectedId, shownTeam, showFunction, hideFunction, shownMediaType }) {
    return (<Box>
        <Box><b>Instance {instanceId}</b> <Switch checked={false}/> automatically</Box>
        <Box>Shown team: {shownTeam}</Box>
        <Box>Media type: {shownMediaType}</Box>
        <Box sx={{ pt: 1 }}>
            <TeamViewSettingsPanel isSomethingSelected={selectedId !== undefined}
                isPossibleToHide={shownTeam !== undefined} onShowTeam={showFunction} onHideTeam={hideFunction}/>
        </Box>
    </Box>);
}

SplitScreenInstance.propTypes = {
    instanceId: PropTypes.any.isRequired,
    selectedId: PropTypes.any,
    shownTeam: PropTypes.string,
    shownMediaType: PropTypes.any,
    showFunction: PropTypes.func.isRequired,
    hideFunction: PropTypes.func.isRequired,
};

const instances = ["TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"];
function SplitScreenGrid({ apiPath, createErrorHandler }) {
    const apiGet = createApiGet(BASE_URL_BACKEND + apiPath);
    const apiPost = createApiPost(BASE_URL_BACKEND + apiPath);

    const [state, setState] = useState({
        dataElements: [],
        selectedId: undefined,
        instances: [],
    });

    const updateData = () => {
        Promise.all([
            Promise.all(instances.map(i => apiGet("/" + i).catch(createErrorHandler("Failed to load list of teams")))),
            apiGet("/teams").catch(createErrorHandler("Failed to load list of teams")),
        ]).then(([inst, teams]) => {
            const showedTeams = inst.filter(i => i?.shown).map(i => i?.settings?.teamId);
            const teamsData = teams.map((elem) => {
                elem.shown = showedTeams.includes(elem.id);
                elem.selected = elem.id === state.selectedId;
                return elem;
            });
            setState(s => ({
                ...s,
                dataElements: teamsData,
                instances: inst,
            }));
        });
    };
    useEffect(updateData, []);

    const selectTeam = (id) => {
        if (id === state.selectedId) {
            id = undefined;
        }
        setState(s => ({
            ...s,
            dataElements: s.dataElements.map((elem) => ({
                ...elem,
                selected: (id === elem.id)
            })),
            selectedId: id
        }));
    };

    const showOnInstance = (instanceId) => (mediaType = undefined) => {
        apiPost("/" + instanceId + "/show_with_settings", { teamId: state.selectedId, mediaType: mediaType })
            .then(setState(s => ({ ...s, selectedId: undefined })))
            .then(updateData);
    };

    const hideOnInstance = (instanceId) => () => {
        apiPost("/" + instanceId + "/hide").then(updateData);
    };
    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column" }}>
        <Grid container rowSpacing={1} columnSpacing={{ xs: 1, sm: 2, md: 3 }} sx={{ width: { "md": "75%", "sm": "100%", "xs": "100%" } }}>
            {(instances).map(id =>
                <Grid item md={6} sm={12} key={id}>
                    <SplitScreenInstance instanceId={id} selectedId={state.selectedId} shownMediaType={state.instances[id]?.shown ?
                        state.instances[id]?.settings?.mediaType : undefined}
                    shownTeam={state.instances[id]?.shown ?
                        state.dataElements.find(t => t.id === state.instances[id]?.settings?.teamId)?.name : undefined}
                    showFunction={showOnInstance(id)} hideFunction={hideOnInstance(id)}
                    />
                </Grid>
            )}
        </Grid>
        <SelectTeamTable teams={state.dataElements} onClickHandler={selectTeam}/>
    </Grid>);
}

SplitScreenGrid.propTypes = {
    apiPath: PropTypes.string.isRequired,
    createErrorHandler: PropTypes.func.isRequired,
};

function SplitScreen() {
    const { enqueueSnackbar, } = useSnackbar();
    return (
        <Container maxWidth="100%" sx={{ pt: 2 }}>
            <SplitScreenGrid apiPath={"/splitScreen"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default SplitScreen;
