import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import { grey } from "@mui/material/colors";

import ShowButton from "./ShowButton";
import { BACKEND_API_URL } from "./config";

// Controls
// const onClickEdit = (currentRow) => () => {
//     if (currentRow.state.editValue === undefined) {
//         currentRow.setState(state => ({ ...state, editValue: state.value }));
//     } else {
//         const requestOptions = {
//             method: "POST",
//             headers: { "Content-Type": "application/json" },
//             body: JSON.stringify({ id: currentRow.props.row.id, text: currentRow.props.row.text })
//         };
//         fetch(BACKEND_API_URL + "/advertisement/" + currentRow.props.row.id, requestOptions)
//             .then(response => response.json())
//             .then(console.log);
//         currentRow.setState(state => ({ ...state, editValue: undefined }));
//     }
// };

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
                    onClick={() => this.setState((state) => ({ ...state, active: !state.active }))}
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
