import React from "react";
import { ControlsTableRow } from "./ControlsTableRow";
import { Table, TableBody } from "@mui/material";

export class ControlsTable extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <Table align="center" aria-label="simple table" sx={{ maxWidth: "sm" }}>
                <TableBody>
                    {this.props.items !== undefined &&
                    this.props.items.map(row =>
                        <ControlsTableRow
                            updateTable={() => {this.props.updateTable();}}
                            activeColor={this.props.activeColor}
                            inactiveColor={this.props.inactiveColor}
                            row={row}
                            key={row.text}
                            keys={this.props.keys}/>)
                    }
                </TableBody>
            </Table>
        );
    }
}
