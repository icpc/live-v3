import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

class TitleTable extends PresetsTable {
}

TitleTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/title",
    apiTableKeys: ["preset", "data"],
    tableKeysHeaders: ["Preset", "Data"]
};

function Title() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Title">
            <TitleTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Title;
