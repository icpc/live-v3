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
        <Container maxWidth="100%" sx={{ pt: 2 }}>
            <TeamViewManager variant="pvp" service={service}/>
        </Container>
    );
}

export default TeamPVP;
