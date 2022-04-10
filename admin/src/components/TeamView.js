import React from "react";
import "../App.css";
import { TeamTable } from "./TeamTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

function TeamView() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <div className="TeamTable">
            <TeamTable
                apiPath="/teamview"
                apiTableKeys={["id", "name"]}
                createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </div>
    );
}

export default TeamView;
