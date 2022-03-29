import React from "react";
import {TableCell, TableRow, TextField} from "@mui/material";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import VisibilityIcon from "@mui/icons-material/Visibility";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import {BACKEND_API_URL} from "./config";
import ShowButton from "./ShowButton";

const onClickEdit = (currentRow) => () => {
    console.log("anbodsfcdsa")
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({...state, editValue: state.value}));
    } else {
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({id: currentRow.props.row.id, text: currentRow.props.row.text})
        };
        fetch(BACKEND_API_URL + "/advertisement/" + currentRow.props.row.id, requestOptions)
            .then(response => response.json())
            .then(console.log);
        currentRow.setState(state => ({...state, editValue: undefined}));
    }
}

export class PresetsTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            "value": props.row,
            "editValue": undefined,
        };
    }

    render() {
        const currentRow = this;
        return (<TableRow
            key={this.state.value["id"]}
            sx={{'&:last-child td, &:last-child th': {border: 0}}}
        >
            <TableCell component="th" scope="row" align={"left"}>
                <ShowButton onClick={() => {console.log("aboba");}}/>
            </TableCell>
            {this.props.keys.map((rowKey) => (
                <TableCell component="th" scope="row" sx={{flexGrow: 1}} key={rowKey}>
                    {this.state.editValue === undefined ? this.state.value[rowKey] : (
                        <TextField
                            hiddenLabel
                            id="filled-hidden-label-small"
                            defaultValue={this.state.value[rowKey]}
                            size="small"
                            onChange={(e) => {
                                currentRow.state.editValue[rowKey] = e.target.value;
                            }}
                        />)
                    }
                </TableCell>
            ))}
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box sx={{ "& > button": { m: 1 } }}>
                    <Button size="small" onClick={onClickEdit(currentRow)}><EditIcon/></Button>
                    <Button size="small" color="error"><DeleteIcon/></Button>
                </Box>
            </TableCell>
        </TableRow>);
    }
}