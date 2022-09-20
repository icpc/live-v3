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
        <Container sx={{ pt: 2 }}>
            <TeamViewManager service={service}/>
        </Container>
    );
}

export default TeamView;
