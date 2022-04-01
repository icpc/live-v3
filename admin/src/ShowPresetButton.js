import React from "react";

import "./App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import IconButton from "@mui/material/IconButton";

import { BACKEND_API_URL } from "./config";

const getUrl = (currentRow) => {
    return (
        BACKEND_API_URL +
        (currentRow.props.path === undefined ? "" : currentRow.props.path + "/") +
        currentRow.props.row.id
    );
};

export const onClickShow = (currentRow) => () => {
    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
    };
    fetch(getUrl(currentRow) + (currentRow.state.active ? "/hide" : "/show"), requestOptions)
        .then(response => response.json())
        .then(() => currentRow.setState(state => ({ ...state, active: !currentRow.state.active })))
        .then(currentRow.props.updateTable)
        .catch(currentRow.props.onErrorHandle("Failed to hide preset"));
};

export class ShowPresetButton extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.active) {
            return <IconButton color="primary" onClick={this.props.onClick}><VisibilityIcon/></IconButton>;
        } else {
            return <IconButton color="primary" onClick={this.props.onClick}><VisibilityOffIcon/></IconButton>;
        }
    }
}
