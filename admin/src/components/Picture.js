import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { PictureTableRow } from "./PictureTableRow";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { PresetsManager } from "./PresetsManager";


export class PictureManager extends PresetsManager {}
PictureManager.defaultProps = {
    ...PresetsManager.defaultProps,
    apiPath: "/picture",
    tableKeys: ["name", "url"],
    rowComponent: PictureTableRow,
};

function Picture() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Pictures">
            <PictureManager createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Picture;
