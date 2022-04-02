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
import { BACKEND_API_URL } from "./config";

const onClickEdit = (currentRow) => () => {
    if (currentRow.state.editValue === undefined) {
        currentRow.setState(state => ({ ...state, editValue: state.value }));
    } else {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: currentRow.state.editValue.content.name,
                url: currentRow.props.row.content.url }),
        };
        fetch(BACKEND_API_URL + currentRow.props.path + "/" + currentRow.props.row.id, requestOptions)
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
    fetch(BACKEND_API_URL + currentRow.props.path + "/" + currentRow.props.row.id + "/delete", requestOptions)
        .then(response => response.json())
        .then(currentRow.setState(state => ({ ...state, editValue: undefined })))
        .then(currentRow.props.updateTable)
        .then(console.log);
};

export class PictureTableRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: props.row,
            editValue: undefined,
            active: props.row.widgetId !== null
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
                            backgroundColor: (this.state.active ? this.props.activeColor : this.props.inactiveColor ) }} >
                        <CardMedia
                            sx={{ display: "flex", flexDirection: "column", maxBlockSize: "sm" }}
                            component="img"
                            image={currentRow.props.row.content.url}
                        />
                        <CardContent>
                            <Typography gutterBottom variant="h6" component="div">
                                {this.props.keys.map((rowKey) => (
                                    <div key={rowKey}>
                                        {this.state.editValue === undefined ? this.state.value.content[rowKey] : (
                                            <Box onSubmit={onClickEdit(currentRow)} component="form" type="submit">
                                                <TextField
                                                    autoFocus
                                                    hiddenLabel
                                                    defaultValue={this.state.value.content[rowKey]}
                                                    id="filled-hidden-label-small"
                                                    type="text"
                                                    size="small"
                                                    onChange={(e) => {
                                                        currentRow.state.editValue.content[rowKey] = e.target.value;
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
                                active={this.state.active}
                            />
                            <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"} onClick={onClickEdit(currentRow)}>
                                {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                            </IconButton>
                            <IconButton color="error" onClick={onClickDelete(currentRow)}><DeleteIcon/></IconButton>
                        </Box>
                    </Card>
                </Box>
            </TableCell>
            {/* {this.props.keys.map((rowKey) => (
                <TableCell
                    component="th"
                    scope="row"
                    key={rowKey}
                    sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                    {this.state.editValue === undefined ? this.state.value.content[rowKey] : (
                        <Box onSubmit={onClickEdit(currentRow)} component="form" type="submit">
                            <TextField
                                hiddenLabel
                                defaultValue={this.state.value.content[rowKey]}
                                id="filled-hidden-label-small"
                                type="text"
                                size="small"
                                onChange={(e) => {
                                    currentRow.state.editValue.content[rowKey] = e.target.value;
                                }}
                            />
                        </Box>)
                    }
                </TableCell>
            ))} */}
            {/* <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color="inherit" onClick={onClickEdit(currentRow)}><EditIcon/></IconButton>
                    <IconButton color="error" onClick={onClickDelete(currentRow)}><DeleteIcon/></IconButton>
                </Box>
            </TableCell> */}
        </TableRow>);
    }
}
