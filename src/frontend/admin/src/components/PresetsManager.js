import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import { PresetsTableRow } from "./PresetsTableRow";
import { AbstractWidgetImpl } from "../services/abstractWidgetImpl";

export function DefaultAddPresetButton({ onCreate }) {
    return (<IconButton color="primary" size="large" onClick={() => onCreate()}><AddIcon/></IconButton>);
}
DefaultAddPresetButton.propTypes = { onCreate: PropTypes.func.isRequired };

export function PresetsManager({ service, RowComponent, defaultRowData, tableKeys, tableKeysHeaders, rowsFilter, AddButtons, isImmutable }) {
    defaultRowData = defaultRowData ?? tableKeys.reduce((ac, key) => ({ ...ac, [key]: "" }), {});
    const [elements, setElements] = useState([]);
    const loadData = () => service.loadPresets().then(setElements);
    const onCreate = (rowData = defaultRowData) => service.createPreset(rowData);
    const onEdit = (data) => service.editPreset(data.id, data.settings);
    const onDelete = (id) => service.deletePreset(id);
    const onShow = ({ shown, id }) => (shown ? service.hidePreset(id) : service.showPreset(id));

    useEffect(loadData, []);
    useEffect(() => {
        service.addReloadDataHandler(loadData);
        return () => service.deleteReloadDataHandler(loadData);
    }, []);

    return (<div>
        <Table align={"center"}>
            {tableKeysHeaders !== undefined &&
                <TableHead>
                    <TableRow>
                        <TableCell key="__show_btn_row__"/>
                        {tableKeysHeaders.map(row =>
                            <TableCell key={row} sx={{ fontWeight: "bold" }}>{row}</TableCell>)}
                        <TableCell/>
                    </TableRow>
                </TableHead>}
            <TableBody>
                {elements !== undefined &&
                    elements.filter(rowsFilter).map((row) =>
                        <RowComponent
                            key={row.id}
                            data={row}
                            tableKeys={tableKeys}
                            onEdit={onEdit}
                            onDelete={() => onDelete(row.id)}
                            onShow={() => onShow(row)}
                            isImmutable={isImmutable}
                        />)}
            </TableBody>
        </Table>
        {isImmutable !== true && <AddButtons onCreate={onCreate}/>}
    </div>);
}

PresetsManager.propTypes = {
    service: PropTypes.instanceOf(AbstractWidgetImpl).isRequired,
    RowComponent: PropTypes.elementType,
    defaultRowData: PropTypes.object,
    tableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    tableKeysHeaders: PropTypes.arrayOf(PropTypes.string),
    rowsFilter: PropTypes.func,
    AddButtons: PropTypes.elementType,
    isImmutable: PropTypes.bool,
};

PresetsManager.defaultProps = {
    RowComponent: PresetsTableRow,
    rowsFilter: () => true,
    AddButtons: DefaultAddPresetButton,
};
