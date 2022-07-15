import React, { useMemo, useState } from "react";
import Container from "@mui/material/Container";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { TableCell, TableRow, TextField } from "@mui/material";
import ShowPresetButton from "./ShowPresetButton";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import { PresetsTableCell } from "./PresetsTableCell";
import PropTypes from "prop-types";
import { activeRowColor } from "../styles";
import { onChangeFieldHandler } from "./PresetsTableRow";
import { PresetsManager } from "./PresetsManager";
import { PresetWidgetService } from "../services/presetWidget";

const parseJSONOrDefault = (text, defult) => {
    try {
        return JSON.parse(text);
    } catch (e) {
        return defult;
    }
};

const ParamsLine = ({ pKey, pValue }) => (<Box sx={{}}><b>{pKey}</b>: {JSON.stringify(pValue)}</Box>);

ParamsLine.propTypes = {
    pKey: PropTypes.string.isRequired,
    pValue: PropTypes.any.isRequired,
};

const ParamsDataEditor = ({ onSubmitAction, value, onChangeHandler }) => (
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

ParamsDataEditor.propTypes = {
    value: PropTypes.any.isRequired,
    onChangeHandler: PropTypes.func.isRequired,
    onSubmitAction: PropTypes.func.isRequired,
};

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
            onChangeValue={onChangeFieldHandler(setEditData, "preset")} onSubmitAction={onSubmitEdit}/>
        <PresetsTableCell value={data.settings.data} editValue={editData?.settings?.data}
            isActive={data.shown} ValueEditor={ParamsDataEditor}
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
    const service = useMemo(() =>
        new PresetWidgetService("/title", errorHandlerWithSnackbar(enqueueSnackbar)), []);
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
