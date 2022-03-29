import React from "react";
import { PresetsTableRow } from "./PresetsTableRow";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";

export class PresetsTable extends React.Component {
    constructor(props) {
        super(props);
    }

    renderHeader() {
        return (
            <TableRow key="header">
                {this.props.headers.map((row) => (
                    <TableCell component="th" scope="row" key={row}>{row}</TableCell>
                ))}
            </TableRow>
        );
    }

    render() {
        return (
            <Table sx={{ minWidth: 650 }} aria-label="simple table">
                <TableHead>
                    {this.renderHeader()}
                </TableHead>
                <TableBody>
                    {this.props.items !== undefined &&
                    this.props.items.map((row) =>
                        <PresetsTableRow row={row} key={row.id} keys={this.props.keys}/>)
                    }
                </TableBody>
            </Table>
        );
    }
}
