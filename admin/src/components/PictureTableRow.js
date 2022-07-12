import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SaveIcon from "@mui/icons-material/Save";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardMedia from "@mui/material/CardMedia";
import Typography from "@mui/material/Typography";

import ShowPresetButton from "./ShowPresetButton";
import { PresetsTableRowOld } from "./PresetsTableRowOld";

export class PictureTableRow extends PresetsTableRowOld {
    render() {
        return (<TableRow key={this.state.value["id"]}>
            <TableCell component="th" scope="row" key="Picture card" sx={{ p: 1, border: 0 }}>
                <Card sx={{ display: "flex", alignItems: "center",
                    backgroundColor: (this.props.rowData.shown ? this.props.tStyle.activeColor : this.props.tStyle.inactiveColor) }}>
                    <CardMedia sx={{ display: "flex", width: "25%" }} component="img"
                        image={this.props.rowData.settings.url}/>
                    <Box sx={{ display: "flex", width: "75%", flexDirection: "column" }}>
                        <CardContent sx={{ pl: 2 }} onSubmit={this.onClickEdit}>
                            {this.state.editValue === undefined &&
                            <Typography gutterBottom variant="h6" sx={{ mb: 0 }}>{this.props.rowData.settings.name}</Typography>}
                            {this.state.editValue === undefined &&
                            <Typography gutterBottom variant="caption">{this.props.rowData.settings.url}</Typography>}

                            {this.state.editValue !== undefined &&
                            this.props.apiTableKeys.map((rowKey) => (
                                <Box onSubmit={this.onSubmitEdit} component="form" type="submit" key={rowKey}>
                                    <TextField
                                        autoFocus
                                        fullWidth
                                        defaultValue={this.props.rowData.settings[rowKey]}
                                        type="text"
                                        size="small"
                                        onChange={(e) => {
                                            this.state.editValue.settings[rowKey] = e.target.value;
                                        }}
                                    />
                                </Box>
                            ))}
                        </CardContent>
                        <Box sx={{ display: "flex", pl: 1, pr: 1, justifyContent: "center" }}>
                            <ShowPresetButton
                                onClick={this.onClickShow}
                                active={this.props.rowData.shown}
                            />
                            <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                                onClick={this.onClickEdit}>
                                {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                            </IconButton>
                            <IconButton color="error" onClick={this.onClickDelete}><DeleteIcon/></IconButton>
                        </Box>
                    </Box>
                </Card>
            </TableCell>
        </TableRow>);
    }
}
