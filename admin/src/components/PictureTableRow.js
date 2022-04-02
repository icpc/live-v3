import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SaveIcon from "@mui/icons-material/Save";
import { grey } from "@mui/material/colors";

import Card from "@mui/material/Card";
import CardActions from "@mui/material/CardActions";
import CardContent from "@mui/material/CardContent";
import CardMedia from "@mui/material/CardMedia";
import Typography from "@mui/material/Typography";

import { ShowPresetButton, onClickShow } from "./ShowPresetButton";
import { BASE_URL_BACKEND } from "../config";

const onClickEdit = (currentRow) => () => {
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({ ...state, editValue: state.value }));
    } else {
        console.log(currentRow.state.editValue);
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: getSettings(currentRow.state.editValue).name,
                url: getSettings(currentRow.props.row).url }),
        };
        fetch(BASE_URL_BACKEND + currentRow.props.path + "/" + currentRow.props.row.id, requestOptions)
            .then(response => response.json())
            .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
            .then(currentRow.props.updateTable)
            .then(console.log);
    }
};

const onClickDelete = (currentRow) => () => {
    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    };
    fetch(BASE_URL_BACKEND + currentRow.props.path + "/" + currentRow.props.row.id + "/delete", requestOptions)
        .then(response => response.json())
        .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
        .then(currentRow.props.updateTable)
        .then(console.log);
};

const getSettings = (row) => {
    return row.settings;
};

export class PictureTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.row,
            editValue: undefined,
            shown: props.row.shown
        };
    }

    render() {
        const currentRow = this;
        return (<TableRow
            key={this.state.value["id"]}>
            <TableCell component="th" scope="row" key="Image card">
                <Box>
                    <Card
                        sx={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            backgroundColor: (this.state.shown ? this.props.activeColor : this.props.inactiveColor ) }} >
                        <CardMedia
                            sx={{ display: "flex", flexDirection: "column", maxBlockSize: "sm" }}
                            component="img"
                            image={getSettings(currentRow.props.row).url}
                        />
                        <CardContent>
                            <Typography gutterBottom variant="h6" component="div">
                                {this.props.keys.map((rowKey) => (
                                    <div key={rowKey}>
                                        {this.state.editValue === undefined ? getSettings(currentRow.props.row)[rowKey] : (
                                            <Box onSubmit={onClickEdit(currentRow)} component="form" type="submit">
                                                <TextField
                                                    autoFocus
                                                    hiddenLabel
                                                    defaultValue={getSettings(currentRow.props.row)[rowKey]}
                                                    id="filled-hidden-label-small"
                                                    type="text"
                                                    size="small"
                                                    onChange={(e) => {
                                                        getSettings(currentRow.state.editValue)[rowKey] = e.target.value;
                                                    }}
                                                />
                                            </Box>)
                                        }
                                    </div>
                                ))}
                            </Typography>
                        </CardContent>
                        <Box sx={{ display: "flex", alignItems: "center", pl: 1, pb: 1 }}>
                            <ShowPresetButton
                                onClick={onClickShow(currentRow)}
                                active={this.state.shown}
                            />
                            <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"} onClick={onClickEdit(currentRow)}>
                                {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                            </IconButton>
                            <IconButton color="error" onClick={onClickDelete(currentRow)}><DeleteIcon/></IconButton>
                        </Box>
                    </Card>
                </Box>
            </TableCell>
        </TableRow>);
    }
}
