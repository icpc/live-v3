import React from "react";
import IconButton from "@mui/material/IconButton";
import AddIcon from "@mui/icons-material/Add";

import { ControlsTable } from "./ControlsTable";
import { BACKEND_API_URL } from "./config";

export default class ControlsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [
            { text: "Scoreboard", id: "/scoreboard" },
            { text: "Queue", id: "/queue" },
            { text: "Ticker", id: "/ticker" }] };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    openAddForm() {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: "React Hooks POST Request Example" })
        };
        fetch(BACKEND_API_URL + "/advertisement", requestOptions)
            .then(response => response.json())
            .then(console.log);
    }

    render() {
        return (
            <div>
                <ControlsTable
                    activeColor={this.props.activeColor}
                    inactiveColor={this.props.inactiveColor}
                    items={this.state.items}
                    headers={["Text"]}
                    keys={["text"]}/>
                <div>
                    <IconButton color="primary" size="large" onClick={this.openAddForm}><AddIcon/></IconButton>
                </div>
            </div>
        );
    }

    handleChange(e) {
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

