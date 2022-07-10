import React from "react";
import Container from "@mui/material/Container";

import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { PresetsManager } from "./PresetsManager";

class AdvertisementManager extends PresetsManager {
}
AdvertisementManager.defaultProps = {
    ...PresetsManager.defaultProps,
    apiPath: "/advertisement",
    tableKeys: ["text"],
};

function Advertisement() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }}>
            <AdvertisementManager createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Advertisement;
