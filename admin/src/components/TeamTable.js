import React from "react";
import PropTypes from "prop-types";
import { Grid, Box, Button, TextField } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import { Team } from "./Team";
import { BASE_URL_BACKEND } from "../config";

const gridButton = {
    margin: "4px"
};

const showButtonsSettings = [
    { text: "camera", mediaType: "camera" },
    { text: "screen", mediaType: "screen" },
    { text: "record", mediaType: "record" },
    { text: "info", mediaType: undefined },];

export class TeamTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = { dataElements: [],
            loaded: false,
            searchFieldValue: "",
            selectedId: undefined,
            shownId: undefined,
            shownMediaType: null };
        this.updateData = this.updateData.bind(this);
        this.showTeam = this.showTeam.bind(this);
        this.hideTeam = this.hideTeam.bind(this);
        this.apiUrl = this.apiUrl.bind(this);
        this.handleSearchFieldChange = this.handleSearchFieldChange.bind(this);
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

    async updateData() {
        const [stat, response] = await Promise.all([
            fetch(this.apiUrl())
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams")),
            fetch(this.apiUrl() + "/info")
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams"))
        ]);
        const result = response.map((elem) => {
            elem.shown = (stat.shown && stat.settings.teamId === elem.id);
            elem.selected = false;
            return elem;
        });
        this.setState({ ...this.state,
            dataElements: result,
            shownMediaType: (stat.shown ? stat.settings.mediaType : null),
            shownId: (stat.shown ? stat.settings.teamId : undefined)
        });
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

    // getDefaultRowData() {
    //     return this.props.apiTableKeys.reduce((ac, key) => ({ ...ac, [key]: "" }), {});
    // }

    rowsFilter(elem) {
        if (this.state.searchFieldValue === "") {
            return true;
        }
        return elem.name.toLowerCase().includes(this.state.searchFieldValue.toLowerCase());
    }

    handleSearchFieldChange(elem) {
        this.setState({ searchFieldValue : elem.target.value });
    }

    async showTeam(mediaType = undefined) {
        await this.apiPost("/show", { teamId: this.state.selectedId, mediaType });
        await this.updateData();
        this.setState({ ...this.state, selectedId: undefined });
    }

    async hideTeam() {
        await this.apiPost("/hide");
        await this.updateData();
        this.setState({ ...this.stat, selectedId: undefined });
    }

    render() {
        const RowComponent = this.props.rowComponent;
        return (
            <Grid sx={{
                display: "flex",
                alignContent: "center",
                justifyContent: "center",
                alignItems: "center",
                flexDirection: "column" }}>
                <Box container sx={{
                    display: "flex",
                    width: "100%",
                    flexWrap: "wrap-reverse",
                    alignContent: "center",
                    justifyContent: "center",
                    alignItems: "center",
                    flexDirection: "row" }}>
                    <Box>
                        {showButtonsSettings.map((elem) => (
                            <Button
                                disabled={this.state.selectedId === undefined && this.state.shownId === undefined}
                                sx={{ ...gridButton,
                                    backgroundColor: (this.state.shownMediaType === elem.mediaType ? "#1976d2" : "primary")
                                }}
                                variant={this.state.shownMediaType === elem.mediaType ? "contained" : "outlined"}
                                key={elem.text}
                                onClick={() => {this.showTeam(elem.mediaType);}}>{elem.text}</Button>
                        ))}
                        <Button
                            sx={gridButton}
                            disabled={this.state.shownId === undefined}
                            variant={this.state.shownId === undefined ? "outlined" : "contained"}
                            color="error"
                            onClick={() => {this.hideTeam();}}>hide</Button>
                    </Box>
                    <TextField
                        onChange={this.handleSearchFieldChange}
                        value={this.state.searchFieldValue}
                        id="Search field"
                        size="small"
                        margin="none"
                        label="Search"
                        variant="outlined"
                        InputProps={{
                            style: { height: "36.5px" }
                        }}
                    />
                </Box>
                <Box sx={{
                    display: "grid",
                    width: { "md": "140%", "sm": "100%", "xs": "100%" },
                    gridTemplateColumns: { "md": "repeat(4, 6fr)", "sm": "repeat(2, 6fr)", "xs": "repeat(1, 6fr)" },
                    gap: 0.25 }}>
                    {this.state.dataElements !== undefined &&
                    this.state.dataElements.filter((r) => this.rowsFilter(r)).map((row) =>
                        <RowComponent
                            apiPostFunc={this.apiPost.bind(this)}
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
        selectedColor: grey.A200,
        activeColor: lightBlue[100],
        inactiveColor: "white",
    },
    rowComponent: Team,
    createErrorHandler: () => () => {},
};
