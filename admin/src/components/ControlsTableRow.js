import React from "react";
import { TableCell, TableRow } from "@mui/material";
import { grey } from "@mui/material/colors";

import { ShowWidgetButton, onClickShow } from "./ShowWidgetButton";

export class ControlsTableRow extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const currentRow = this;
        return (<TableRow
            sx={{ backgroundColor: (this.props.row.shown ? this.props.activeColor : this.props.inactiveColor ), maxWidth: "sm" }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowWidgetButton
                    onClick={
                        () => {
                            onClickShow(currentRow);
                            this.setState({ shown: !this.props.row.shown });
                            currentRow.props.updateTable();
                        }
                    }
                    active={this.props.row.shown}
                />
            </TableCell>
            {this.props.keys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    align="left"
                    sx={{ color: (this.props.row.shown ? grey[900] : grey[700]) }}>
                    { this.props.row[rowKey] }
                </TableCell>
            ))}
        </TableRow>);
    }
}
