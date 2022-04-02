import React from "react";
import { lightBlue } from "@mui/material/colors";

import { PictureTable } from "./PictureTable";
import { BASE_URL_BACKEND } from "../config";

import FormDialog from "./PictureDIalog";

export default class PicturePanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [] };
        this.update = this.update.bind(this);
        this.addPictureRequest = this.addPictureRequest.bind(this);
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

    addPictureRequest(name="", url="") {
        const keys = [...this.props.tableKeys, "url"];
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ "name": name, "url": url })
        };
        fetch(BASE_URL_BACKEND + this.props.path, requestOptions)
            .then(response => response.json())
            .then(this.update)
            .catch(this.onErrorHandle("Failed to add picture"));
    }

    render() {
        return (
            <div>
                <PictureTable
                    path={this.props.path}
                    updateTable={() => {this.update();}}
                    activeColor={this.props.activeColor}
                    inactiveColor={this.props.inactiveColor}
                    items={this.state.items}
                    keys={this.props.tableKeys}
                    onErrorHandle={this.onErrorHandle}/>
                <div>

                </div>
                <FormDialog addRequest={this.addPictureRequest}/>
            </div>
        );
    }
}

PicturePanel.defaultProps = {
    ...PicturePanel.defaultProps,
    path: "/picture",
    tableKeys: ["name"],
    activeColor: lightBlue[100],
    inactiveColor: "white",
};
