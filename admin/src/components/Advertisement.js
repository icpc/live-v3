import React, { useMemo } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { PresetsManager } from "./PresetsManager";
import { PresetWidgetService } from "../services/presetWidget";

function Advertisement() {
    const { enqueueSnackbar,  } = useSnackbar();
    const service = useMemo(() =>
        new PresetWidgetService("/advertisement", errorHandlerWithSnackbar(enqueueSnackbar)), []);
    return (
        <Container maxWidth="md" sx={{ pt: 2 }}>
            <PresetsManager service={service} tableKeys={["text"]}/>
        </Container>
    );
}

export default Advertisement;
