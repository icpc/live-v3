import React from "react";
import "../App.css";
import { TeamTable } from "./TeamTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

// export class TeamView extends TeamTable {
//     constructor(props) {
//         super(props);
//     }
// }

// TeamView.defaultProps = {
//     ...TeamTable.defaultProps,
//     apiPath: "/teamview",
//     apiTableKeys: ["name", "url"],
//     rowComponent: Card,
// };

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
