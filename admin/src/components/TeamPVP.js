import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { TeamTable } from "./TeamTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";

class TeamPVPTable extends TeamTable {
    constructor(props) {
        super(props);
        this.state = { ...this.state, selectedIds: [] };
    }

    isTeamShown(stat, id) {
        return stat.shown && stat.settings.teamId !== undefined && stat.settings.teamId.includes(id);
    }

    selectItem(id) {
        let selectedIds = this.state.selectedIds;
        if (selectedIds.includes(id)) {
            selectedIds = selectedIds.filter(x => x !== id);
        } else {
            selectedIds.push(id);
        }
        selectedIds = selectedIds.slice(-2);
        const newDataElements = this.state.dataElements.map((elem) => ({
            ...elem,
            selected: selectedIds.includes(elem.id)
        }));
        this.setState(state => ({ ...state, dataElements: newDataElements, selectedIds: selectedIds,
            selectedId: (selectedIds.length === 2 ? selectedIds : undefined) }));
    }
}

function TeamPVP() {
    const { enqueueSnackbar,  } = useSnackbar();
    return (
        <Container maxWidth="100%" sx={{ pt: 2 }} className="TeamTable">
            <TeamPVPTable
                apiPath="/teamPVP"
                createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default TeamPVP;
