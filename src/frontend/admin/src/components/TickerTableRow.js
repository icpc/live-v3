import React, { useState } from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import ImageIcon from "@mui/icons-material/Image";
import ShowPresetButton from "./ShowPresetButton";
import { activeRowColor } from "../styles";
import PropTypes from "prop-types";
import { onChangeFieldEventHandler } from "./PresetsTableRow";

export function TickerTableRow({ data, onShow, onEdit, onDelete }) {
    const [editData, setEditData] = useState();

    const onClickEdit = () => {
        if (editData === undefined) {
            setEditData(() => data);
        } else {
            onEdit(editData).then(() => setEditData(undefined));
        }
    };
    const onSubmitEdit = (e) => {
        e.preventDefault();
        onClickEdit();
    };

    return (
        <TableRow key={data.id} sx={{ backgroundColor: (data.shown ? activeRowColor : undefined) }}>
            <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
                <ShowPresetButton onClick={onShow} active={data.shown}/>
            </TableCell>
            <TableCell component="th" scope="row">
                {data.settings.type === "clock" && <ClockIcon/>}
                {data.settings.type === "scoreboard" && <ScoreboardIcon/>}
                {data.settings.type === "text" && <TextIcon/>}
                {data.settings.type === "image" && <ImageIcon/>}
            </TableCell>
            <TableCell component="th" scope="row">
                {data.settings.type === "clock" &&
                    (editData === undefined ? data.settings.timeZone : (
                        <Box onSubmit={onSubmitEdit} component="form" type="submit">
                            <TextField autoFocus hiddenLabel fullWidth defaultValue={data.settings.timeZone}
                                id="filled-hidden-label-small" type="text" size="small" sx={{ width: 1 }}
                                onChange={onChangeFieldEventHandler(setEditData, "timeZone")}/>
                        </Box>)
                    )}
                {data.settings.type === "text" &&
                    (editData === undefined ? data.settings.text : (
                        <Box onSubmit={onSubmitEdit} component="form" type="submit">
                            <TextField autoFocus hiddenLabel fullWidth defaultValue={data.settings.text}
                                id="filled-hidden-label-small" type="text" size="small" sx={{ width: 1 }}
                                onChange={onChangeFieldEventHandler(setEditData, "text")}/>
                        </Box>)
                    )}
                {data.settings.type === "image" &&
                    (editData === undefined ? data.settings.text : (
                        <Box onSubmit={onSubmitEdit} component="form" type="submit">
                            <TextField autoFocus hiddenLabel fullWidth defaultValue={data.settings.path}
                                id="filled-hidden-label-small" type="text" size="small" sx={{ width: 1 }}
                                onChange={onChangeFieldEventHandler(setEditData, "path")}/>
                        </Box>)
                    )}
                {data.settings.type === "scoreboard" &&
                    (editData === undefined ?
                        "From " + data.settings.from + " to " + data.settings.to :
                        (<Box onSubmit={onSubmitEdit} component="form" type="submit" sx={{ display: "flex", flexDirection: "row" }}>
                            <TextField autoFocus hiddenLabel fullWidth defaultValue={data.settings.from}
                                id="filled-hidden-label-small" type="number" size="small" sx={{ width: 0.49 }}
                                onChange={onChangeFieldEventHandler(setEditData, "from")}/>
                            <TextField autoFocus hiddenLabel fullWidth defaultValue={data.settings.to}
                                id="filled-hidden-label-small" type="number" size="small" sx={{ width: 0.49 }}
                                onChange={onChangeFieldEventHandler(setEditData, "to")}/>
                        </Box>)
                    )}
            </TableCell>
            <TableCell component="th" scope="row">
                {editData === undefined ? data.settings.periodMs : (
                    <Box onSubmit={onSubmitEdit} component="form" type="submit">
                        <TextField autoFocus hiddenLabel defaultValue={data.settings.periodMs}
                            id="filled-hidden-label-small" type="number" size="small"
                            onChange={onChangeFieldEventHandler(setEditData, "periodMs")}/>
                    </Box>)
                }
            </TableCell>
            <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                <Box>
                    <IconButton color={editData === undefined ? "inherit" : "primary"} onClick={() => { onClickEdit(); }}>
                        {editData === undefined ? <EditIcon/> : <SaveIcon/>}
                    </IconButton>
                    <IconButton color="error" onClick={onDelete}><DeleteIcon/></IconButton>
                </Box>
            </TableCell>
        </TableRow>);
}
TickerTableRow.propTypes = {
    data: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.object.isRequired,
    }),
    onShow: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
};
