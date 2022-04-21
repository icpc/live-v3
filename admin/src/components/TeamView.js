import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { TeamTable } from "./TeamTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

function TeamView() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="100%" sx={{ pt: 2 }} className="TeamTable">
            <TeamTable
                apiPath="/teamView"
                createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default TeamView;
