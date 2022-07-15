import React, { useMemo } from "react";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { ControlsWidgetService } from "../services/controlsWidget";
import { PresetsManager } from "./PresetsManager";


function Controls() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = useMemo(() => new ControlsWidgetService(errorHandlerWithSnackbar(enqueueSnackbar)), []);
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Controls">
            <PresetsManager service={service} tableKeys={["text"]}/>
        </Container>
    );
}

export default Controls;
