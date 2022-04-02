import React from "react";
import { ControlsTable } from "./ControlsTable";
import { BASE_URL_BACKEND } from "../config";

export default class ControlsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            loaded: false,
            items: [
                { text: "Scoreboard", path: "/scoreboard", active: false },
                { text: "Queue", path: "/queue", active: false },
                { text: "Ticker", path: "/ticker", active: false },
                { text: "Statistics", path: "/statistics", active: false }] };
    }

    async update() {
        const newItems = await Promise.all(
            this.state.items.map(async (element) => {
                let result = await fetch(BASE_URL_BACKEND + element.path);
                result = await result.json();
                try {
                    return { ...element, ...result };
                } catch {
                    return { ...element };
                }
            })
        );
        this.setState({ items: newItems, loaded: true });
    }

    async componentDidMount() {
        await this.update();
    }

    render() {
        console.log(this.state);
        return (
            <div>
                { this.state.loaded && <ControlsTable
                    key="controls table"
                    updateTable={() => {this.update();}}
                    activeColor={this.props.activeColor}
                    inactiveColor={this.props.inactiveColor}
                    items={this.state.items}
                    headers={["Text"]}
                    keys={["text"]}/>
                }
            </div>
        );
    }
}

