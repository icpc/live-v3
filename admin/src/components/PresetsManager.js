import React from "react";
import PropTypes from "prop-types";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import { PresetWidgetService } from "../services/presetWidget";
import { PresetsTableRow } from "./PresetsTableRow";

export class PresetsManager extends React.Component {
    constructor(props) {
        super(props);
        this.state = { dataElements: [] };
        this.service = this.props.service ?? new PresetWidgetService(this.props.apiPath, this.props.createErrorHandler);
        this.loadData = () => {
            this.service.loadPresets().then((result) => {
                this.setState(state => ({
                    ...state,
                    dataElements: result,
                }));
            });
        };
        this.onCreate = (rowData = this.getDefaultRowData()) => {
            return this.service.createPreset(rowData);
        };
        this.onEdit = (data) => {
            console.log(data);
            return this.service.editPreset(data.id, data.settings);
        };
        this.onDelete = (id) => {
            return this.service.deletePreset(id);
        };
        this.onShow = ({ shown, id }) => {
            return (shown ? this.service.hidePreset(id) : this.service.showPreset(id));
        };
    }

    componentDidMount() {
        this.loadData();
        this.service.setReloadDataHandler(this.loadData);
    }

    getDefaultRowData() {
        return this.props.tableKeys.reduce((ac, key) => ({ ...ac, [key]: "" }), {});
    }

    rowsFilter() {
        return true;
    }

    renderAddButton() {
        return (<IconButton color="primary" size="large" onClick={() => this.onCreate()}><AddIcon/></IconButton>);
    }

    render() {
        const RowComponent = this.props.rowComponent;
        return (<div>
            <Table align={"center"}>
                {this.props.tableKeysHeaders !== undefined &&
                <TableHead>
                    <TableRow>
                        <TableCell key="__show_btn_row__"/>
                        {this.props.tableKeysHeaders.map(row =>
                            <TableCell key={row} sx={{ fontWeight: "bold" }}>{row}</TableCell>)}
                        <TableCell/>
                    </TableRow>
                </TableHead>}
                <TableBody>
                    {this.state.dataElements !== undefined &&
                    this.state.dataElements.filter((r) => this.rowsFilter(r)).map((row) =>
                        <RowComponent
                            key={row.id}
                            data={row}
                            tableKeys={this.props.tableKeys}
                            onEdit={this.onEdit}
                            onDelete={() => this.onDelete(row.id)}
                            onShow={() => this.onShow(row)}
                            isImmutable={this.props.isImmutable}
                        />)}
                </TableBody>
            </Table>
            {this.props.isImmutable !== true && this.renderAddButton()}
        </div>);
    }
}
PresetsManager.propTypes = {
    apiPath: PropTypes.string,
    service: PropTypes.shape({
        showPreset: PropTypes.func,
        hidePreset: PropTypes.func,
    }),
    tableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    tableKeysHeaders: PropTypes.arrayOf(PropTypes.string),
    rowComponent: PropTypes.elementType,
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};

PresetsManager.defaultProps = {
    rowComponent: PresetsTableRow,
    createErrorHandler: () => () => {},
};
