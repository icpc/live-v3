import React from "react";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { useControlsWidgetService } from "../services/controlsWidget";
import { PresetsManager } from "./PresetsManager";


function Controls() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = useControlsWidgetService(errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Controls">
            <PresetsManager service={service} tableKeys={["text"]} isImmutable={true}/>
        </Container>
    );
}

export default Controls;
