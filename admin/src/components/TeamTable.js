import React from "react";
import PropTypes from "prop-types";
import { Grid, Box, Button } from "@mui/material";
import { lightBlue } from "@mui/material/colors";
import { Team } from "./Team";
import { BASE_URL_BACKEND } from "../config";

const gridButton = {
    margin: "8px"
};

export class TeamTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = { dataElements: [], loaded: false, selectedId: undefined };
        this.updateData = this.updateData.bind(this);
        this.showTeam = this.showTeam.bind(this);
        this.hideTeam = this.hideTeam.bind(this);
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
        if (id == this.state.selectedId) {
            id = undefined;
        }
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

    async showTeam(mediaType = undefined) {
        await this.apiPost("/show", { teamId: this.state.selectedId, mediaType });
        await this.updateData();
    }

    async hideTeam() {
        await this.apiPost("/hide");
        await this.updateData();
    }

    render() {
        console.log(this.state);
        const RowComponent = this.props.rowComponent;
        return (
            <Grid sx={{ display: "flex", flexDirection: "column", maxWidth: "xl", mx: "auto" }}>
                <Box container sx={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", maxWidth: "xl", mx: "auto" }}>
                    <Button disabled={this.state.selectedId === undefined} sx={gridButton} variant="contained" onClick={
                        () => {this.showTeam("camera");}}>Camera</Button>
                    <Button disabled={this.state.selectedId === undefined} sx={gridButton} variant="contained" onClick={
                        () => {this.showTeam("screen");}}>Screen</Button>
                    <Button disabled={this.state.selectedId === undefined} sx={gridButton} variant="contained" onClick={
                        () => {this.showTeam("record");}}>Record</Button>
                    <Button disabled={this.state.selectedId === undefined} sx={gridButton} variant="contained" onClick={
                        () => {this.showTeam();}}>Statistics</Button>
                    <Button sx={gridButton} variant="contained" color="error" onClick={
                        () => {this.hideTeam();}}>Hide</Button>
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
