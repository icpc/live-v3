import React from "react";
import PropTypes from "prop-types";
import { TableCell, TableRow, Grid } from "@mui/material";
import Box from "@mui/material/Box";
import { grey } from "@mui/material/colors";
import ShowPresetButton from "./ShowPresetButton";

const getSettings = (row) => {
    return row;
};

export class Team extends React.Component {
    constructor(props) {
        super(props);
        this.onClickEdit = this.onClickEdit.bind(this);
        this.onClickDelete = this.onClickDelete.bind(this);
        this.onClickShow = this.onClickShow.bind(this);
    }

    onClickEdit() {
        if (this.state.editValue === undefined) {
            this.setState(state => ({ ...state, editValue: state.value }));
        } else {
            this.props.apiPostFunc("/" + this.props.rowData.id, getSettings(this.state.editValue))
                .then(() => this.setState(state => ({ ...state, editValue: undefined })))
                .then(this.props.updateTable)
                .catch(this.props.createErrorHandler("Failed to edit preset"));
        }
    }

    onClickDelete() {
        this.props.apiPostFunc("/" + this.props.rowData.id, {}, "DELETE")
            .then(this.props.updateTable)
            .catch(this.props.createErrorHandler("Failed to delete preset"));
    }

    onClickShow() {
        this.props.apiPostFunc(
            "/" + (this.props.rowData.shown ? "/hide" : "/show"),
            { teamId: this.props.rowData.id })
            .then(this.props.updateTable)
            .catch(this.props.createErrorHandler("Failed to show or hide preset"));
    }

    render() {
        return (<Grid>
            <TableRow
                key={this.props.rowData.id}
                sx={{ backgroundColor:
                    (this.props.rowData.shown?
                        (this.props.rowData.selected?
                            this.props.tStyle.selectedActiveColor :
                            this.props.tStyle.activeColor) :
                        this.props.rowData.selected ?
                            this.props.tStyle.selectedColor :
                            this.props.tStyle.inactiveColor)
                }}
                onClick={() => this.props.onClick(this.props.rowData.id)}>
                {this.props.apiTableKeys.map((rowKey) => (
                    <TableCell
                        component="th"
                        scope="row"
                        key={rowKey}
                        sx={{ color: (this.props.rowData.selected || this.props.rowData.shown ? grey[900] : grey[700]) }}>
                        {getSettings(this.props.rowData)[rowKey]}
                    </TableCell>
                ))}
            </TableRow>
        </Grid>);
    }
}

Team.propTypes = {
    apiPostFunc: PropTypes.func.isRequired,
    apiTableKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateTable: PropTypes.func.isRequired,
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
        selectedColor: PropTypes.string,
        selectedActiveColor: PropTypes.string,
    }).isRequired,
    rowData: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
        shown: PropTypes.bool.isRequired,
        selected: PropTypes.bool.isRequired,
    }),
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};
