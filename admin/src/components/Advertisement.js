import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

class AdvertisementTable extends PresetsTable {
}

AdvertisementTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/advertisement",
    apiTableKeys: ["text"],
};

function Advertisement() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Advertisement">
            <AdvertisementTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Advertisement;
