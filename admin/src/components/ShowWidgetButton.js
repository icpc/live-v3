import React from "react";

import "../App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import IconButton from "@mui/material/IconButton";

import { BASE_URL_BACKEND } from "../config";

const getUrl = (currentRow) => {
    return (BASE_URL_BACKEND + currentRow.props.row.path);
};

const show = (currentRow) => {
    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({})
    };
    fetch(getUrl(currentRow) + "/show", requestOptions)
        .then(response => response.json())
        .then(console.log);
};

const hide = (currentRow) => {
    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({})
    };
    fetch(getUrl(currentRow) + "/hide", requestOptions)
        .then(response => response.json())
        .then(console.log);
};

export const onClickShow = (currentRow) => {
    if (currentRow.props.row.active) {
        hide(currentRow);
    } else {
        show(currentRow);
    }
    if (!(currentRow.props.updateTable === undefined)) {
        currentRow.props.updateTable();
    }
};

export class ShowWidgetButton extends React.Component{
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

ShowWidgetButton.defaultProps = {
    ...ShowWidgetButton.defaultProps,
    onClick: onClickShow
};

