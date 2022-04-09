import React from "react";
import PropTypes from "prop-types";
import { Table, TableBody, TableCell, TableHead, TableRow, Grid, Box, Button } from "@mui/material";
import { lightBlue } from "@mui/material/colors";
import AddIcon from "@mui/icons-material/Add";
import IconButton from "@mui/material/IconButton";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import { Team } from "./Team";
import { BASE_URL_BACKEND } from "../config";

export class TeamTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = { dataElements: [], loaded: false, openShowForm: false, selectedId: undefined };
        this.updateData = this.updateData.bind(this);
        this.openShowForm = this.openShowForm.bind(this);
        this.closeShowForm = this.closeShowForm.bind(this);
        this.showTeam = this.showTeam.bind(this);
    }

    apiUrl() {
        return BASE_URL_BACKEND + this.props.apiPath;
    }

    apiPost(path, body = {}, method = "POST") {
        console.log(body);
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

    async updateData() {
        const stat = await (await fetch(this.apiUrl())).json();
        let result = await (await fetch(this.apiUrl() + "/info")).json();
        result = await result.map((elem) => {
            elem.shown = (stat.shown && stat.settings.teamId === elem.id);
            elem.selected = false;
            return elem;
        });
        this.setState({ ...this.state, dataElements: result });
        // .catch(this.props.createErrorHandler("Failed to load list of teams"));
    }

    selectItem(id) {
        const newDataElements = this.state.dataElements.map((elem) => ({
            ...elem,
            selected: (id === elem.id)
        }));
        this.setState({ ...this.state, dataElements: newDataElements, selectedId: id });
    }

    componentDidMount() {
        this.updateData();
    }

    getDefaultRowData() {
        return this.props.apiTableKeys.reduce((ac, key) => ({ ...ac, [key]: "" }), {});
    }

    rowsFilter() {
        return true;
    }

    openShowForm() {
        this.setState({ ...this.state, openShowForm: true });
    }

    closeShowForm() {
        this.setState({ ...this.state, openShowForm: false });
    }

    async showTeam(mediaType = undefined) {
        await this.apiPost("/show", { teamId: this.state.selectedId, mediaType });
        await this.updateData();
        await this.closeShowForm();
    }

    render() {
        console.log(this.state);
        const RowComponent = this.props.rowComponent;
        return (
            <Grid sx={{ display: "flex", flexDirection: "column", maxWidth: "xl", mx: "auto" }}>
                <Dialog
                    open={this.state.openShowForm}
                    onClose={this.closeShowForm}
                    aria-labelledby="alert-dialog-title"
                    aria-describedby="alert-dialog-description"
                    maxWidth="xl"
                >
                    <DialogTitle id="alert-dialog-title">
                        {"TeamView"}
                    </DialogTitle>
                    <DialogContent>
                        <DialogContentText id="alert-dialog-description">
                            Choose what type of media you wish to show.
                        </DialogContentText>
                    </DialogContent>
                    <DialogActions sx={{ display: "grid", gridTemplateColumns: "repeat(2, 2fr)", gap: 2 }}>
                        <Button onClick={
                            () => {this.showTeam("camera");}}>Camera</Button>
                        <Button onClick={
                            () => {this.showTeam("screen");}}>Screen</Button>
                        <Button onClick={
                            () => {this.showTeam("record");}}>Record</Button>
                        <Button onClick={
                            () => {this.showTeam();}}>Statistics</Button>
                        <Button color="error" onClick={this.closeShowForm} autoFocus>
                            Close
                        </Button>
                    </DialogActions>
                </Dialog>
                <Box container display="flex" justifyContent="center">
                    <Button variant="contained" onClick={this.openShowForm}>SHOW</Button>
                </Box>
                <Box sx={{ display: "grid", maxWidth: "xl", gridTemplateColumns: "repeat(3, 6fr)", gap: 2 }}>
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
                            onClick={(id) => this.selectItem(id)}
                        />)}
                </Box>
            </Grid>);
    }
}

TeamTable.propTypes = {
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

TeamTable.defaultProps = {
    tStyle: {
        selectedColor: lightBlue[50],
        activeColor: lightBlue[100],
        selectedActiveColor: lightBlue[200],
        inactiveColor: "white",
    },
    rowComponent: Team,
    createErrorHandler: () => () => {},
};
