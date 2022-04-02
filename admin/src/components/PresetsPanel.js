import React from "react";
import IconButton from "@mui/material/IconButton";
import AddIcon from "@mui/icons-material/Add";

import { PresetsTable } from "./PresetsTable";
import { BASE_URL_BACKEND } from "../config";
import { Alert, Snackbar } from "@mui/material";
import { lightBlue } from "@mui/material/colors";

export default class PresetsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [] };
        this.update = this.update.bind(this);
        this.onErrorHandle = this.onErrorHandle.bind(this);
        this.hideErrorAlert = this.hideErrorAlert.bind(this);
    }

    onErrorHandle(cause) {
        return (error) => {
            console.log(cause + ":", error);
            this.setState(state => ({
                ...state,
                error: cause,
            }));
        };
    }

    hideErrorAlert() {
        this.setState(state => ({ ...state, error: undefined }));
    }

    update() {
        fetch(BASE_URL_BACKEND + this.props.path)
            .then(res => res.json())
            .then(
                (result) => {
                    this.setState(state => ({
                        ...state,
                        items: result,
                        error: undefined,
                    }));
                })
            .catch(this.onErrorHandle("Failed to load list of presets"));
    }

    componentDidMount() {
        this.update();
    }

    addPresetRequest() {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(this.props.tableKeys.reduce((defaultObj, key) => {
                defaultObj[key] = "";
                return defaultObj;
            }, {}))
        };
        fetch(BASE_URL_BACKEND + this.props.path, requestOptions)
            .then(response => response.json())
            .then(this.update)
            .catch(this.onErrorHandle("Failed to add preset"));
    }

    render() {
        return (
            <div>
                <PresetsTable
                    path={this.props.path}
                    updateTable={() => {
                        this.update();
                    }}
                    activeColor={this.props.activeColor}
                    inactiveColor={this.props.inactiveColor}
                    items={this.state.items}
                    tableKeys={this.props.tableKeys}
                    onErrorHandle={this.onErrorHandle}/>
                <IconButton color="primary" size="large" onClick={() => {
                    this.addPresetRequest();
                }}><AddIcon/></IconButton>

                <Snackbar open={this.state.error !== undefined} autoHideDuration={10000}
                    anchorOrigin={{ vertical: "bottom", horizontal: "right" }}>
                    <Alert onClose={this.hideErrorAlert} severity="error" sx={{ width: "100%" }}>
                        {this.state.error}
                    </Alert>
                </Snackbar>
            </div>
        );
    }
}

PresetsPanel.defaultProps = {
    ...PresetsPanel.defaultProps,
    activeColor: lightBlue[100],
    inactiveColor: "white",
};


