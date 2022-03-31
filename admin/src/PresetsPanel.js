import React from "react";
import Button from "@mui/material/Button";
import AddIcon from "@mui/icons-material/Add";

import { PresetsTable } from "./PresetsTable";
import { PresetsTableRow } from "./PresetsTableRow";
import { BACKEND_API_URL } from "./config";

export default class PresetsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [] };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.update = this.update.bind(this);
    }

    update() {
        console.log("update\n");
        fetch(BACKEND_API_URL + this.props.path)
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
                    <Button type="submit" size="large" onClick={() => {this.openAddForm();}}><AddIcon/></Button>
                </div>
            </div>
        );
    }

    handleChange(e) {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: "React Hooks POST Request Example" })
        };
        fetch(BACKEND_API_URL + this.props.path, requestOptions)
            .then(response => response.json())
            .then(console.log);
        this.setState({ text: e.target.value });
    }

    handleSubmit(e) {
        e.preventDefault();
        if (this.state.text.length === 0) {
            return;
        }
        const newItem = {
            text: this.state.text,
            id: Date.now()
        };
        this.setState(state => ({
            items: state.items.concat(newItem),
        }));
    }
}

