import React from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { useTeamViewService } from "../services/teamViewWidget";
import { TeamViewManager } from "./TeamViewManager";


function TeamPVP() {
    const { enqueueSnackbar,  } = useSnackbar();
    const service = useTeamViewService("pvp", errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container sx={{ pt: 2 }}>
            <TeamViewManager service={service}/>
        </Container>
    );
}

export default TeamPVP;
