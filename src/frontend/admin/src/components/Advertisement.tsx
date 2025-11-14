import React from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { PresetsManager } from "./PresetsManager";
import { usePresetWidgetService } from "../services/presetWidget";

function Advertisement(): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const service = usePresetWidgetService(
        "/advertisement",
        errorHandlerWithSnackbar(enqueueSnackbar),
    );

    return (
        <Container maxWidth="lg" sx={{ pt: 2 }}>
            <PresetsManager service={service} tableKeys={["text"]} />
        </Container>
    );
}

export default Advertisement;
