import { Grid, Switch } from "@mui/material";
import React, { useEffect, useState } from "react";
import { useSnackbar } from "notistack";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import Box from "@mui/material/Box";
import { SelectTeamTable, ChooseMediaTypeAndShowPanel } from "./TeamTable";
import PropTypes from "prop-types";
import { BASE_URL_BACKEND } from "../config";
import { createApiGet, createApiPost } from "../utils";


function SplitScreenInstance({ instanceId, selectedId, shownTeam }) {
    return (<Box>
        <Box>Instance {instanceId}</Box>
        <Box>Shown team: {shownTeam}</Box>
        <Box>Automatically <Switch checked={false}/></Box>
        <ChooseMediaTypeAndShowPanel isSomethingSelected={selectedId !== undefined}
            isPossibleToHide={false} showTeamFunction={() => []} hideTeamFunction={() => []}/>
    </Box>);
}
SplitScreenInstance.propTypes = {
    instanceId: PropTypes.any.isRequired,
    selectedId: PropTypes.any,
    shownTeam: PropTypes.string,
};

function SplitScreenGrid({ apiPath, createErrorHandler }) {
    const apiGet = createApiGet(BASE_URL_BACKEND + apiPath);
    const apiPost = createApiPost(BASE_URL_BACKEND + apiPath);

    const [state, setState] = useState({
        dataElements: [],
        selectedId: undefined,
    });

    const updateData = () => {
        Promise.all([
            apiGet("/")
                .catch(createErrorHandler("Failed to load list of teams")),
            apiGet("/info")
                .catch(createErrorHandler("Failed to load list of teams")),
        ]).then(([stat, response]) => {
            let stat2 = { "shown":true,"settings":[{ "teamId":18,"mediaType":"camera" }, { "teamId":22,"mediaType":"camera" }] };
            const teamsData = response.map((elem) => {
                elem.shown = stat2.settings.some(({ teamId }) => teamId === elem.id);
                elem.selected = elem.id === state.selectedId;
                return elem;
            });
            window.d = teamsData;
            setState(s => ({ ...s,
                dataElements: teamsData,
                shownElements: (stat2.shown ? stat2.settings : undefined)
            }));
        });
    };
    useEffect(updateData, []);

    const selectTeam = (id) => {
        if (id === state.selectedId) {
            id = undefined;
        }
        setState(s => ({ ...s,
            dataElements: s.dataElements.map((elem) => ({
                ...elem,
                selected: (id === elem.id)
            })),
            selectedId: id }));
    };

    return (
        <Box>
            <Grid container rowSpacing={1} columnSpacing={{ xs: 1, sm: 2, md: 3 }}>
                {([0, 1, 2, 3]).map(id =>
                    <Grid item xs={6} key={id}>
                        <SplitScreenInstance instanceId={id + 1} selectedId={state.selectedId}
                            shownTeam={Array.isArray(state.shownElements) ?
                                state.dataElements.find(t => t.id === state.shownElements[id]?.teamId)?.name : undefined }/>
                    </Grid>
                )}
            </Grid>
            <SelectTeamTable teams={state.dataElements} onClickHandler={selectTeam}/>
        </Box>
    );
}
SplitScreenGrid.propTypes = {
    apiPath: PropTypes.string.isRequired,
    createErrorHandler: PropTypes.func.isRequired,
};

function SplitScreen() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="100%" sx={{ pt: 2 }} className="TeamTable">
            <SplitScreenGrid apiPath={"/teamView"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default SplitScreen;
