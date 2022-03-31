import React from "react";
import Button from "@mui/material/Button";
import AddIcon from "@mui/icons-material/Add";

import { ControlsTable } from "./ControlsTable";
import { BACKEND_API_URL } from "./config";

export default class ControlsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [{ text: "Scoreboard" }, { text: "Queue" }, { text: "Ticker" }] };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    // componentDidMount() {
    //     fetch(BACKEND_API_URL + "/advertisement")
    //         .then(res => res.json())
    //         .then(
    //             (result) => {
    //                 console.log(this.state);
    //                 console.log("result", result);
    //                 this.setState({
    //                     isLoaded: true,
    //                     items: result,
    //                 });
    //             },
    //             (error) => {
    //                 this.setState({
    //                     isLoaded: true,
    //                     error
    //                 });
    //             }
    //         );
    // }

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
                    activeColor={ this.props.activeColor }
                    inactiveColor={ this.props.inactiveColor }
                    items={this.state.items}
                    headers={["Text"]}
                    keys={["text"]}/>
                <div>
                    <Button variant="text" size="large" onClick={this.openAddForm}><AddIcon/></Button>
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

