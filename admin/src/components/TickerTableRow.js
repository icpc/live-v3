import React from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import { grey } from "@mui/material/colors";
import ShowPresetButton from "./ShowPresetButton";
import { PresetsTableRow } from "./PresetsTableRow";

export class TickerTableRow extends PresetsTableRow {
    render() {
        return (<TableRow key={this.state.value.id}
            sx={{ backgroundColor: (this.props.rowData.shown ? this.props.tStyle.activeColor : this.props.tStyle.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"}>
                <ShowPresetButton onClick={this.onClickShow} active={this.props.rowData.shown}/>
            </TableCell>
            <TableCell component="th" scope="row">
                {this.state.value.settings.type === "clock" && <ClockIcon/>}
                {this.state.value.settings.type === "scoreboard" && <ScoreboardIcon/>}
                {this.state.value.settings.type === "text" && <TextIcon/>}
            </TableCell>
            <TableCell component="th" scope="row" sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                {this.state.value.settings.type === "text" &&
                (this.state.editValue === undefined ? this.state.value.settings.text : (
                    <Box onSubmit={this.onSubmitEdit} component="form" type="submit">
                        <TextField autoFocus hiddenLabel fullWidth defaultValue={this.state.value.settings.text}
                            id="filled-hidden-label-small" type="text" size="small" sx={{ width: 1 }}
                            onChange={(e) => {
                                this.state.editValue.settings.text = e.target.value;
                            }}
                        />
                    </Box>)
                )}
                {this.state.value.settings.type === "scoreboard" &&
                (this.state.editValue === undefined ?
                    "From " + this.state.value.settings.from + " to " + this.state.value.settings.to
                    : (<Box onSubmit={this.onSubmitEdit} component="form" type="submit" sx={{ display: "flex", flexDirection: "row" }}>
                        <TextField autoFocus hiddenLabel fullWidth defaultValue={this.state.value.settings.from}
                            id="filled-hidden-label-small" type="number" size="small" sx={{ width: 0.49 }}
                            onChange={(e) => {
                                this.state.editValue.settings.from = e.target.value;
                            }}/>
                        <TextField autoFocus hiddenLabel fullWidth defaultValue={this.state.value.settings.to}
                            id="filled-hidden-label-small" type="number" size="small" sx={{ width: 0.49 }}
                            onChange={(e) => {
                                this.state.editValue.settings.to = e.target.value;
                            }}/>
                    </Box>)
                )}
            </TableCell>
            <TableCell component="th" scope="row" sx={{ color: (this.state.active ? grey[900] : grey[700]) }}>
                {this.state.editValue === undefined ? this.state.value.settings.periodMs : (
                    <Box onSubmit={this.onSubmitEdit} component="form" type="submit">
                        <TextField autoFocus hiddenLabel defaultValue={this.state.value.settings.periodMs}
                            id="filled-hidden-label-small" type="number" size="small"
                            onChange={(e) => {
                                this.state.editValue.settings.periodMs = e.target.value;
                            }}
                        />
                    </Box>)
                }
            </TableCell>
            <TableCell component="th" scope="row" align={"right"}>
                <Box>
                    <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                        onClick={this.onClickEdit}>
                        {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={this.onClickDelete}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>
        </TableRow>);
    }
}
