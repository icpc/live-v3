import React from "react";
import { PictureTableRow } from "./PictureTableRow";
import { Table, TableBody } from "@mui/material";

export class PictureTable extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <Table align="center" aria-label="simple table" sx={{ maxWidth: "sm" }}>
                {/* <PictureList items={this.props.items}/> */}
                <TableBody align="center" sx={{ alignItems: "center" }}>
                    {this.props.items.map((row) =>
                        <PictureTableRow
                            path={this.props.path}
                            updateTable={() => {this.props.updateTable();}}
                            activeColor={this.props.activeColor}
                            inactiveColor={this.props.inactiveColor}
                            row={row}
                            key={row.id}
                            keys={this.props.keys}
                            onErrorHandle={this.props.onErrorHandle}/>)
                    }
                </TableBody>
            </Table>
        );
    }
}
