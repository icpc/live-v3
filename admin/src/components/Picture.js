import React from "react";
import Container from "@mui/material/Container";
import { PictureTableRow } from "./PictureTableRow";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { PresetsManager } from "./PresetsManager";
import { usePresetWidgetService } from "../services/presetWidget";


function Picture() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = usePresetWidgetService("/picture", errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Pictures">
            <PresetsManager service={service} tableKeys={["name", "url"]} RowComponent={PictureTableRow}/>
        </Container>
    );
}

export default Picture;
