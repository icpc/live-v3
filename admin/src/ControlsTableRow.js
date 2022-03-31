import React from "react";
import { TableCell, TableRow } from "@mui/material";
import { grey } from "@mui/material/colors";

import { ShowButton, onClickShow } from "./ShowButton";

export class ControlsTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.row,
            active: false
        };
    }

    render() {
        const currentRow = this;
        return (<TableRow
            key={this.state.value["id"]}
            sx={{ backgroundColor: (this.state.active ? this.props.activeColor : this.props.inactiveColor ), maxWidth: "sm" }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowButton
                    onClick={
                        () => {
                            console.log(this.state.value);
                            onClickShow(currentRow);
                            this.setState({ active: !this.state.active });
                        }
                    }
                    active={this.state.active}
                />
            </TableCell>
            {this.props.keys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    align="left"
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    { this.state.value[rowKey] }
                </TableCell>
            ))}
        </TableRow>);
    }
}
