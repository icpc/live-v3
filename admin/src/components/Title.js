import React, { useEffect, useState } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { Autocomplete, TextField, Button,
    TableCell, TableRow,
    Dialog, DialogTitle, DialogContent, DialogActions,
    CircularProgress, Card, Stack } from "@mui/material";
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
import { usePresetTableRowDataState } from "./PresetsTableRow";
import { PresetsManager } from "./PresetsManager";
import { usePresetWidgetService } from "../services/presetWidget";
import { useTitleWidgetService } from "../services/titleWidget";
import { useDebounce } from "../utils";

const PreviewSVGDialog = ({ id, ...props }) => {
    const { enqueueSnackbar } = useSnackbar();
    const service = useTitleWidgetService("/title", errorHandlerWithSnackbar(enqueueSnackbar), false);
    const [content, setContent] = useState();
    useEffect(() => props.open && service.getPreview(id).then(r => setContent(r.content)), [props.open, id]);
    return (
        <Dialog fullWidth maxWidth="md" { ...props }>
            <DialogTitle>Title preview</DialogTitle>
            <DialogContent>
                <Card sx={{ borderRadius: 0 }}>
                    {content === undefined &&
                        <Stack alignItems="center" sx={{ py: 3 }}><CircularProgress/></Stack>}
                    {content !== undefined && <object type="image/svg+xml" data={content} style={{ width: "100%" }}/>}
                </Card>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose} autoFocus>OK</Button>
            </DialogActions>
        </Dialog>
    );
};
PreviewSVGDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    id: PropTypes.number.isRequired,
};

const TemplateEditor = ({ value, onSubmit, onChange }) => {
    const [templates, setTemplates] = useState([]);

    const service = useTitleWidgetService("/title", undefined, false);
    useEffect(() => service.getTemplates().then(ts => setTemplates(ts)), []);

    return (<Box onSubmit={onSubmit} component="form" type="submit">
        <Autocomplete
            disablePortal
            freeSolo
            sx={{ width: 1 }}
            size="small"
            value={value}
            onChange={(_, value) => onChange(value) }
            options={templates}
            renderInput={(params) => <TextField {...params} label="SVG preset"/>}/>
    </Box>);
};
TemplateEditor.propTypes = ValueEditorPropTypes;

const ParamsLine = ({ pKey, pValue }) =>
    (<Box><b>{pKey}</b>: {pValue}</Box>);
ParamsLine.propTypes = { pKey: PropTypes.string.isRequired, pValue: PropTypes.any.isRequired };

const paramsDataToString = (value) =>
    Object.entries(value).map(([k, v]) => k + ": " + v).join("\n");

const parseParamsData = input =>
    input.split("\n")
        .map(line => line.split(/:\W?/, 2))
        .filter(r => r.length >= 2)
        .reduce((ac, [ k, v ]) => ({ ...ac, [k]: v }), {});

const ParamsDataEditor = ({ onSubmit, value, onChange }) => {
    const [inputValue, setInputValue] = useState(paramsDataToString(value));
    const debouncedInputValue = useDebounce(inputValue, 250);
    useEffect(() => onChange(parseParamsData(debouncedInputValue)),
        [debouncedInputValue]);
    return (
        <Box onSubmit={onSubmit} component="form" type="submit">
            <TextField
                autoFocus
                multiline
                hiddenLabel
                defaultValue={paramsDataToString(value)}
                id="filled-hidden-label-small"
                type="text"
                size="small"
                sx={{ width: 1 }}
                onChange={(e) => setInputValue(e.target.value)}
            />
        </Box>);
};
ParamsDataEditor.propTypes = ValueEditorPropTypes;

function TitleTableRow({ data, onShow, onEdit, onDelete }) {
    const [editData, onClickEdit, onSubmitEdit, onChangeField] = usePresetTableRowDataState(data, onEdit);
    const [previewDialogOpen, setPreviewDialogOpen] = useState(false);

    const SpecialShow = ({ preset }) =>
        (<Button onClick={(e) => {
            // onClickEdit();
            const nData = { ...data, settings: { ...data.settings, preset: preset } };
            onEdit(nData);
            setTimeout(() => onShow(), 500);
            // onChangeField("preset")("left.svg");
            // onClickEdit();
            // onSubmitEdit(e);
\        }}>{ preset[0] }</Button>);

    return (<TableRow key={data.id} sx={{ backgroundColor: (data.shown ? activeRowColor : undefined) }}>
        <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
            <ShowPresetButton onClick={onShow} active={data.shown}/>
            <SpecialShow preset={"left.svg"}/>
            <SpecialShow preset={"right.svg"}/>
        </TableCell>
        <PresetsTableCell value={data.settings.preset} editValue={editData?.settings?.preset} isActive={data.shown}
            onChange={onChangeField("preset")} onSubmit={onSubmitEdit}
            valueEditor={TemplateEditor}
        />
        <PresetsTableCell value={data.settings.data} editValue={editData?.settings?.data}
            isActive={data.shown} valueEditor={ParamsDataEditor}
            onChange={onChangeField( "data")} onSubmit={onSubmitEdit}
            valuePrinter={(v) => Object.entries(v).map(e => <ParamsLine key={e[0]} pKey={e[0]} pValue={e[1]}/>)}
        />
        <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
            <Box>
                <PreviewSVGDialog
                    open={previewDialogOpen}
                    onClose={() => setPreviewDialogOpen(false)}
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
                tableKeysHeaders={["Template", "Data"]}
                defaultRowData={{ "preset": "", "data": {} }}
                RowComponent={TitleTableRow}
            />
        </Container>
    );
}

export default Title;
