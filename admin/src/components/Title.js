import React, { useEffect, useState } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { Autocomplete, TableCell, TableRow, TextField, Dialog, DialogTitle, DialogContent, DialogActions, Button, Card } from "@mui/material";
import ShowPresetButton from "./ShowPresetButton";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import PreviewIcon from "@mui/icons-material/Preview";
import { PresetsTableCell, ValueEditorPropTypes } from "./PresetsTableCell";
import PropTypes from "prop-types";
import { activeRowColor } from "../styles";
import { onChangeFieldHandler } from "./PresetsTableRow";
import { PresetsManager } from "./PresetsManager";
import { usePresetWidgetService } from "../services/presetWidget";
import { useTitleWidgetService } from "../services/titleWidget";

const PreviewSVGDialog = ({ open, handleClose, id }) => {
    const service = useTitleWidgetService("/title", undefined, false);
    const [content, setContent] = useState(undefined);
    useEffect(() => service.getPreview(id).then(c =>  setContent(c)), []);
    return (
        <Dialog onClose={handleClose} open={open} fullWidth maxWidth="md">
            <DialogTitle>Preset preview</DialogTitle>
            <DialogContent>
                <Card sx={{ borderRadius: 0, width: "sm" }}>
                    <object type="image/svg+xml" data={content} style={{ width: "100%" }}/>
                </Card>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} autoFocus>OK</Button>
            </DialogActions>
        </Dialog>
    );
};

const PresetEditor = ({ value, onSubmitAction, onChangeHandler }) => {
    const [templates, setTemplates] = useState([]);

    const service = useTitleWidgetService("/title", undefined, false);
    useEffect(() => service.getTemplates().then(ts => setTemplates(ts)), []);

    return (<Box onSubmit={onSubmitAction} component="form" type="submit">
        <Autocomplete
            disablePortal
            freeSolo
            sx={{ width: 1 }}
            size="small"
            value={value}
            onChange={(_, value) => onChangeHandler(value) }
            options={templates}
            renderInput={(params) => <TextField {...params} label="SVG preset"/>}/>
    </Box>);
};
PresetEditor.propTypes = ValueEditorPropTypes;

const ParamsLine = ({ pKey, pValue }) => (<Box sx={{}}><b>{pKey}</b>: {pValue}</Box>);
ParamsLine.propTypes = {
    pKey: PropTypes.string.isRequired,
    pValue: PropTypes.any.isRequired,
};

const parseParamsData = input =>
    input.split("\n")
        .map(line => line.split(/:\W?/, 2))
        .filter(r => r.length >= 2)
        .reduce((ac, [ k, v ]) => { ac[k] = v; return ac; }, {});

const YamlParamsDataEditor = ({ onSubmitAction, value, onChangeHandler }) => (
    <Box onSubmit={onSubmitAction} component="form" type="submit">
        <TextField
            autoFocus
            multiline
            hiddenLabel
            defaultValue={Object.entries(value).map(([ k, v ]) => k + ": " + v).join("\n")}
            id="filled-hidden-label-small"
            type="text"
            size="small"
            sx={{ width: 1 }}
            onChange={(e) => onChangeHandler(parseParamsData(e.target.value))}
        />
    </Box>);

YamlParamsDataEditor.propTypes = ValueEditorPropTypes;

function TitleTableRow({ data, onShow, onEdit, onDelete }) {
    const [editData, setEditData] = useState();
    const [previewDialogOpen, setPreviewDialogOpen] = useState(false);

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

    return (<TableRow key={data.id} sx={{ backgroundColor: (data.shown ? activeRowColor : undefined) }}>
        <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
            <ShowPresetButton onClick={onShow} active={data.shown}/>
        </TableCell>
        <PresetsTableCell value={data.settings.preset} editValue={editData?.settings?.preset} isActive={data.shown}
            onChangeValue={onChangeFieldHandler(setEditData, "preset")} onSubmitAction={onSubmitEdit}
            ValueEditor={PresetEditor}
        />
        <PresetsTableCell value={data.settings.data} editValue={editData?.settings?.data}
            isActive={data.shown} ValueEditor={YamlParamsDataEditor}
            onChangeValue={onChangeFieldHandler(setEditData, "data")} onSubmitAction={onSubmitEdit}
            ValuePrinter={(v) => Object.entries(v).map(e => <ParamsLine key={e[0]} pKey={e[0]} pValue={e[1]}/>)}
        />
        <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
            <Box>
                <PreviewSVGDialog
                    open={previewDialogOpen}
                    handleClose={() => setPreviewDialogOpen(false)}
                    id={data.id}
                />
                {editData === undefined && <IconButton onClick={() => setPreviewDialogOpen(true)}><PreviewIcon/></IconButton>}
                <IconButton color={editData === undefined ? "inherit" : "primary"} onClick={onClickEdit}>
                    {editData === undefined ? <EditIcon/> : <SaveIcon/>}
                </IconButton>
                <IconButton color="error" onClick={onDelete}><DeleteIcon/></IconButton>
            </Box>
        </TableCell>
    </TableRow>);
}
TitleTableRow.propTypes = {
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

function Title() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = usePresetWidgetService("/title", errorHandlerWithSnackbar(enqueueSnackbar));
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Title">
            <PresetsManager
                service={service}
                tableKeys={["preset", "data"]}
                tableKeysHeaders={["Preset", "Data"]}
                RowComponent={TitleTableRow}
            />
        </Container>
    );
}

export default Title;
