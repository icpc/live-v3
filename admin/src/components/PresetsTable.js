import React from "react";
import PropTypes from "prop-types";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import { lightBlue } from "@mui/material/colors";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import { BASE_URL_BACKEND } from "../config";
import { PresetsTableRow } from "./PresetsTableRow";

export class PresetsTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = { dataElements: [] };
        this.updateData = this.updateData.bind(this);
    }

    apiUrl() {
        return BASE_URL_BACKEND + this.props.apiPath;
    }

    apiPost(path, body = {}, method = "POST") {
        const requestOptions = {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        };
        return fetch(this.apiUrl() + path, requestOptions)
            .then(response => response.json())
            .then(response => {
                if (response.status !== "ok") {
                    throw new Error("Server return not ok status: " + response);
                }
                return response;
            });
    }

    updateData() {
        fetch(this.apiUrl())
            .then(res => res.json())
            .then(
                (result) => {
                    this.setState(state => ({
                        ...state,
                        dataElements: result,
                    }));
                })
            .catch(this.props.createErrorHandler("Failed to load list of presets"));
    }

    componentDidMount() {
        this.updateData();
    }

    getDefaultRowData() {
        return this.props.apiTableKeys.reduce((ac, key) => ({ ...ac, [key]: "" }), {});
    }

    doAddPreset(rowData = this.getDefaultRowData()) {
        this.apiPost("", rowData)
            .then(this.updateData)
            .catch(this.props.createErrorHandler("Failed to add preset"));
    }

    rowsFilter() {
        return true;
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
                        <TableCell key="__manage_row__"/>
                    </TableRow>
                </TableHead>}
                <TableBody>
                    {this.state.dataElements !== undefined &&
                    this.state.dataElements.filter((r) => this.rowsFilter(r)).map((row) =>
                        <RowComponent
                            apiPostFunc={this.apiPost.bind(this)}
                            apiTableKeys={this.props.apiTableKeys}
                            updateTable={this.updateData}
                            tStyle={this.props.tStyle}
                            rowData={row}
                            key={row.id}
                            createErrorHandler={this.props.createErrorHandler}
                            isImmutable={this.props.isImmutable}
                        />)}
                </TableBody>
            </Table>
            {this.props.isImmutable !== true && this.renderAddButton()}
        </div>);
    }

    renderAddButton() {
        return (<IconButton color="primary" size="large" onClick={() => {
            this.doAddPreset();
        }}><AddIcon/></IconButton>);
    }
}

PresetsTable.propTypes = {
    apiPath: PropTypes.string.isRequired,
    apiTableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    tableKeysHeaders: PropTypes.arrayOf(PropTypes.string),
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
    }),
    rowComponent: PropTypes.elementType,
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};

PresetsTable.defaultProps = {
    tStyle: {
        activeColor: lightBlue[100],
        inactiveColor: "white",
    },
    rowComponent: PresetsTableRow,
    createErrorHandler: () => () => {},
};
