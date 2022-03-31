import React from "react";
import IconButton from "@mui/material/IconButton";
import AddIcon from "@mui/icons-material/Add";

import { PresetsTable } from "./PresetsTable";
import { BACKEND_API_URL } from "./config";

export default class PresetsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [] };
        this.update = this.update.bind(this);
    }

    update() {
        setTimeout ( () => {
            return fetch(BACKEND_API_URL + this.props.path)
                .then(res => res.json())
                .then(
                    (result) => {
                        console.log(this.state);
                        console.log("result", result);
                        this.setState({
                            isLoaded: true,
                            items: result,
                        });
                    },
                    (error) => {
                        this.setState({
                            isLoaded: true,
                            error
                        });
                    }
                );
        },
        15); // Timeout in order to render properly ¯\_(ツ)_/¯
    }

    componentDidMount() {
        this.update();
    }

    openAddForm() {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: "New Advertisement" })
        };
        fetch(BACKEND_API_URL + "/advertisement", requestOptions)
            .then(response => response.json())
            .then(console.log);
        this.update();

    }

    render() {
        return (
            <div>
                <PresetsTable
                    path={this.props.path}
                    updateTable={() => {this.update();}}
                    activeColor={this.props.activeColor}
                    inactiveColor={this.props.inactiveColor}
                    items={this.state.items}
                    keys={["text"]}/>
                <div>
                    <IconButton color="primary" size="large" onClick={() => {this.openAddForm();}}><AddIcon/></IconButton>
                </div>
            </div>
        );
    }
}

