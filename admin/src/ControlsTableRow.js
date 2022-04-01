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
            sx={{ backgroundColor: (this.props.row.active ? this.props.activeColor : this.props.inactiveColor ), maxWidth: "sm" }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowWidgetButton
                    onClick={
                        () => {
                            onClickShow(currentRow);
                            this.setState({ active: !this.props.row.active });
                            currentRow.props.updateTable();
                        }
                    }
                    active={this.props.row.active}
                />
            </TableCell>
            {this.props.keys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    align="left"
                    sx={{ color: (this.props.row.active ? grey[900] : grey[700]) }}>
                    { this.props.row[rowKey] }
                </TableCell>
            ))}
        </TableRow>);
    }
}
