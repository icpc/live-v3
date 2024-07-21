import React, { useState } from "react";
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

import ShowPresetButton from "./controls/ShowPresetButton.tsx";
import { activeRowColor } from "../styles";
import PropTypes from "prop-types";

export function PictureTableRow({ data, onShow, onEdit, onDelete }) {
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

    return (<TableRow key={data.id}>
        <TableCell component="th" scope="row" sx={{ p: 1, border: 0 }}>
            <Card sx={{ display: "flex", alignItems: "center",
                backgroundColor: (data.shown ? activeRowColor : undefined) }}>
                <CardMedia sx={{ display: "flex", width: "25%" }} component="img"
                    image={data.settings.url}/>
                <Box sx={{ display: "flex", width: "75%", flexDirection: "column" }}>
                    <CardContent sx={{ pl: 2 }} onSubmit={onClickEdit}>
                        {editData === undefined &&
                            <Typography gutterBottom variant="h6" sx={{ mb: 0 }}>{data.settings.name}</Typography>}
                        {editData === undefined &&
                            <Typography gutterBottom variant="caption">{data.settings.url}</Typography>}

                        {editData !== undefined &&
                            ["name", "url"].map((rowKey) => (
                                <Box onSubmit={onSubmitEdit} component="form" type="submit" key={rowKey} sx={{ my: 1 }}>
                                    <TextField
                                        autoFocus
                                        fullWidth
                                        defaultValue={editData.settings[rowKey]}
                                        type="text"
                                        size="small"
                                        label={rowKey}
                                        onChange={(e) => {
                                            editData.settings[rowKey] = e.target.value;
                                        }}
                                    />
                                </Box>
                            ))}
                    </CardContent>
                    <Box sx={{ display: "flex", pl: 1, pr: 1, justifyContent: "center" }}>
                        <ShowPresetButton
                            onClick={onShow}
                            checked={data.shown}
                        />
                        <IconButton color={editData === undefined ? "inherit" : "primary"}
                            onClick={onClickEdit}>
                            {editData === undefined ? <EditIcon/> : <SaveIcon/>}
                        </IconButton>
                        <IconButton color="error" onClick={onDelete}><DeleteIcon/></IconButton>
                    </Box>
                </Box>
            </Card>
        </TableCell>
    </TableRow>);
}
PictureTableRow.propTypes = {
    data: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
        shown: PropTypes.bool.isRequired,
        settings: PropTypes.object.isRequired,
    }),
    tableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    onShow: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    isImmutable: PropTypes.bool,
};
