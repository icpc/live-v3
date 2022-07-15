import React, { useEffect, useMemo, useState } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { Autocomplete, Table, TableBody, TableCell, TableRow, TextField } from "@mui/material";
import ShowPresetButton from "./ShowPresetButton";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import { PresetsTableCell, ValueEditorPropTypes } from "./PresetsTableCell";
import PropTypes from "prop-types";
import { activeRowColor } from "../styles";
import { onChangeFieldHandler } from "./PresetsTableRow";
import { PresetsManager } from "./PresetsManager";
import { usePresetWidgetService } from "../services/presetWidget";
import { TitleWidgetService } from "../services/titleWidget";

const parseJSONOrDefault = (text, defult) => {
    try {
        return JSON.parse(text);
    } catch (e) {
        return defult;
    }
};

const PresetEditor = ({ value, onSubmitAction, onChangeHandler }) => {
    const [templates, setTemplates] = useState([]);

    const service = useMemo(() => new TitleWidgetService("/title", undefined, false), []);
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

const ParamsLine = ({ pKey, pValue }) => (<Box sx={{}}><b>{pKey}</b>: {JSON.stringify(pValue)}</Box>);
ParamsLine.propTypes = {
    pKey: PropTypes.string.isRequired,
    pValue: PropTypes.any.isRequired,
};

const JSONParamsDataEditor = ({ onSubmitAction, value, onChangeHandler }) => (
    <Box onSubmit={onSubmitAction} component="form" type="submit">
        <TextField
            autoFocus
            multiline
            hiddenLabel
            defaultValue={JSON.stringify(value, null, " ")}
            id="filled-hidden-label-small"
            type="text"
            size="small"
            sx={{ width: 1 }}
            onChange={(e) => {
                onChangeHandler(parseJSONOrDefault(e.target.value, value));
            }}
        />
    </Box>);

JSONParamsDataEditor.propTypes = {
    value: PropTypes.any.isRequired,
    onChangeHandler: PropTypes.func.isRequired,
    onSubmitAction: PropTypes.func.isRequired,
};

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
            onChange={(e) => {
                onChangeHandler(parseJSONOrDefault(e.target.value, value));
            }}
        />
    </Box>);

YamlParamsDataEditor.propTypes = {
    value: PropTypes.any.isRequired,
    onChangeHandler: PropTypes.func.isRequired,
    onSubmitAction: PropTypes.func.isRequired,
};

const ParamsDataEditorRow = ({ value, onChange, sx }) => (<TextField
    autoFocus
    hiddenLabel
    sx={sx}
    defaultValue={value}
    type="text"
    size="small"
    onChange={(e) => onChange(e.target.value)}
/>);

const ParamsDataEditor = ({ onSubmitAction, value, onChangeHandler }) => {
    const [data, setData] = useState(Object.entries(value).map(([ k, v ], id) => ({ id: id, k: k, v: v })));
    const onChange = (d) => onChangeHandler(d.filter(row => row.v !== "").reduce((ac, { k, v }) => { ac[k] = v; return ac; }, {}));
    const changeKey = (id, newK) => setData(d => { d[id].k = newK; onChange(d); return d; });
    const changeValue = (id, newV) => setData(d => { d[id].v = newV; onChange(d); return d; });
    const addParam = () => setData(d => ([ ...d, { id: d.length, k: "", v: "" }]));
    return (<Box onSubmit={onSubmitAction} component="form" type="submit">
        <Table>
            <TableBody>
                {data.map(({ id, k, v }) =>
                    <TableRow key={id} sx={{ "& td, & th": { border: 0, py: 0.25, px: 0.5 } }}>
                        <TableCell>
                            <ParamsDataEditorRow value={k} onChange={newKey => changeKey(id, newKey)} sx={{ width: 0.4 }}/>
                            <ParamsDataEditorRow value={v} onChange={newValue => changeValue(id, newValue)} sx={{ width: 0.6 }}/>
                        </TableCell>
                    </TableRow>)}
            </TableBody>
        </Table>
        <IconButton color="primary" size="large" onClick={addParam}><AddIcon/></IconButton>
    </Box>);
};
ParamsDataEditor.propTypes = ValueEditorPropTypes;

function TitleTableRow({ data, onShow, onEdit, onDelete }) {
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
