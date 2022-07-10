import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import { PresetsTable } from "./PresetsTable";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import { onChangeSettingCellValue, PresetsTableRowOld } from "./PresetsTableRowOld";
import { TableCell, TableRow, TextField } from "@mui/material";
import ShowPresetButton from "./ShowPresetButton";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import { PresetsTableCell } from "./PresetsTableCell";
import PropTypes from "prop-types";

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

const paramsDataEditor = ({ onSubmitAction, value, onChangeHandler }) => (
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

paramsDataEditor.propTypes = {
    value: PropTypes.any.isRequired,
    onChangeHandler: PropTypes.func.isRequired,
    onSubmitAction: PropTypes.func.isRequired,
};

export class TitleTableRow extends PresetsTableRowOld {
    render() {
        let valueSettings = this.state.value.settings;
        let editValueSettings = this.state.editValue?.settings;
        return (<TableRow
            key={this.state.value.id}
            sx={{ backgroundColor: (this.props.rowData.shown ? this.props.tStyle.activeColor : this.props.tStyle.inactiveColor) }}>
            <TableCell component="th" scope="row" align={"left"} key="__show_btn_row__">
                <ShowPresetButton onClick={this.onClickShow} active={this.props.rowData.shown}/>
            </TableCell>
            <PresetsTableCell value={valueSettings.preset} editValue={editValueSettings?.preset} isActive={this.props.rowData.shown}
                onChangeValue={onChangeSettingCellValue(this, "preset")} onSubmitAction={this.onSubmitEdit}
            />
            <PresetsTableCell value={valueSettings.data} editValue={editValueSettings?.data}
                isActive={this.props.rowData.shown} ValueEditor={paramsDataEditor}
                onChangeValue={onChangeSettingCellValue(this, "data")} onSubmitAction={this.onSubmitEdit}
                ValuePrinter={(v) => Object.entries(v).map(e => <ParamsLine key={e[0]} pKey={e[0]} pValue={e[1]}/>)}
            />
            {this.props.isImmutable !== true &&
                <TableCell component="th" scope="row" align={"right"} key="__manage_row__">
                    <Box>
                        <IconButton color={this.state.editValue === undefined ? "inherit" : "primary"}
                            onClick={this.onClickEdit}>
                            {this.state.editValue === undefined ? <EditIcon/> : <SaveIcon/>}
                        </IconButton>
                        <IconButton color="error" onClick={this.onClickDelete}><DeleteIcon/></IconButton>
                    </Box>
                </TableCell>}
        </TableRow>);
    }
}

class TitleTable extends PresetsTable {
    getDefaultRowData() {
        return { ...super.getDefaultRowData(), "data": {} };
    }
}

TitleTable.defaultProps = {
    ...PresetsTable.defaultProps,
    rowComponent: TitleTableRow,
    apiPath: "/title",
    apiTableKeys: ["preset", "data"],
    tableKeysHeaders: ["Preset", "Data"]
};

function Title() {
    const { enqueueSnackbar, } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Title">
            <TitleTable createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default Title;
