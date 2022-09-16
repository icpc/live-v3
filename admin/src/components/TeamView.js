import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { TeamViewManager } from "./TeamViewManager";
import { useTeamViewService } from "../services/teamViewWidget";

function TeamView() {
    const { enqueueSnackbar,  } = useSnackbar();
    const service = useTeamViewService("singe", errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container maxWidth="100%" sx={{ pt: 2 }}>
            <TeamViewManager variant={"single"} service={service}/>
            {/*<TeamViewTable*/}
            {/*    apiPath="/teamView"*/}
            {/*    createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>*/}
        </Container>
    );
}

export default TeamView;
