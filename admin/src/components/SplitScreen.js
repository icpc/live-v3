import React from "react";
import { useSnackbar } from "notistack";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import { useTeamViewService } from "../services/teamViewWidget";
import { TeamViewManager } from "./TeamViewManager";


function SplitScreen() {
    const { enqueueSnackbar,  } = useSnackbar();
    const service = useTeamViewService("splitScreen", errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container sx={{ pt: 2, width: 1 }}>
            <TeamViewManager variant="splitScreen" service={service}/>
        </Container>
    );
}

export default SplitScreen;
