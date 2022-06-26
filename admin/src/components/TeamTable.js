import React from "react";
import PropTypes from "prop-types";
import { Grid, Box, Button, TextField } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import { Team, TEAM_FIELD_STRUCTURE } from "./Team";
import { BASE_URL_BACKEND } from "../config";

const gridButton = {
    margin: "4px"
};

const showButtonsSettings = [
    { text: "camera", mediaType: "camera" },
    { text: "screen", mediaType: "screen" },
    { text: "record", mediaType: "record" },
    { text: "info", mediaType: undefined },
];

export function ChooseMediaTypeAndShowPanel({ selectedMediaType, isSomethingSelected, isPossibleToHide, showTeamFunction, hideTeamFunction }) {
    return (<Box>
        {showButtonsSettings.map((elem) => (
            <Button
                disabled={!isSomethingSelected}
                sx={{ ...gridButton,
                    backgroundColor: (selectedMediaType === elem.mediaType ? "#1976d2" : "primary")
                }}
                variant={selectedMediaType === elem.mediaType ? "contained" : "outlined"}
                key={elem.text}
                onClick={() => {showTeamFunction(elem.mediaType);}}>{elem.text}</Button>
        ))}
        <Button
            sx={gridButton}
            disabled={!isPossibleToHide}
            variant={!isPossibleToHide ? "outlined" : "contained"}
            color="error"
            onClick={() => hideTeamFunction()}>hide</Button>
    </Box>);
}
ChooseMediaTypeAndShowPanel.propTypes = {
    selectedMediaType: PropTypes.any,
    isSomethingSelected: PropTypes.bool.isRequired,
    isPossibleToHide: PropTypes.bool.isRequired,
    showTeamFunction: PropTypes.func.isRequired,
    hideTeamFunction: PropTypes.func.isRequired,
};

export function SelectTeamTable({ teams, RowComponent, onClickHandler, tStyle }) {
    return (<Box sx={{
        display: "grid",
        width: { "md": "75%", "sm": "100%", "xs": "100%" },
        gridTemplateColumns: { "md": "repeat(4, 6fr)", "sm": "repeat(2, 6fr)", "xs": "repeat(1, 6fr)" },
        gap: 0.25 }}>
        {teams !== undefined && teams.map((row) =>
            <RowComponent
                tStyle={tStyle}
                rowData={row}
                key={row.id}
                onClick={onClickHandler}
            />)}
    </Box>);
}
SelectTeamTable.propTypes = {
    teams: PropTypes.arrayOf(TEAM_FIELD_STRUCTURE),
    onClickHandler: PropTypes.func.isRequired,
    RowComponent: PropTypes.elementType,
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
    }),
};
SelectTeamTable.defaultProps = {
    tStyle: {
        selectedColor: grey.A200,
        activeColor: lightBlue[100],
        inactiveColor: "white",
    },
    RowComponent: Team,
};

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

    isTeamShown(stat, id) {
        return stat.shown && stat.settings.teamId === id;
    }

    updateData() {
        Promise.all([
            fetch(this.apiUrl())
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams")),
            fetch(this.apiUrl() + "/info")
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams"))
        ]).then(([stat, response]) => {
            const teamsData = response.map((elem) => {
                elem.shown = this.isTeamShown(stat, elem.id);
                elem.selected = false;
                return elem;
            });
            this.setState(state => ({ ...state,
                dataElements: teamsData,
                shownMediaType: (stat.shown ? stat.settings.mediaType : null),
                shownId: (stat.shown ? stat.settings.teamId : undefined)
            }));
        });
    }

    selectItem(id) {
        if (id === this.state.selectedId) {
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
        await this.apiPost("/show_with_settings", { teamId: this.state.selectedId, mediaType });
        await this.updateData();
        this.setState({ ...this.state, selectedId: undefined });
    }

    async hideTeam() {
        await this.apiPost("/hide");
        await this.updateData();
        this.setState({ ...this.stat, selectedId: undefined });
    }

    render() {
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
                    <ChooseMediaTypeAndShowPanel isPossibleToHide={this.state.shownId !== undefined}
                        isSomethingSelected={this.state.selectedId !== undefined}
                        selectedMediaType={this.state.shownMediaType} showTeamFunction={this.showTeam} hideTeamFunction={this.hideTeam}/>
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
                <SelectTeamTable teams={this.state.dataElements.filter((r) => this.rowsFilter(r))}
                    RowComponent={this.props.rowComponent} onClickHandler={(id) => this.selectItem(id)}/>
            </Grid>);
    }
}

TeamTable.propTypes = {
    apiPath: PropTypes.string.isRequired,
    tableKeysHeaders: PropTypes.arrayOf(PropTypes.string),
    rowComponent: PropTypes.elementType,
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
};

TeamTable.defaultProps = {
    rowComponent: Team,
    createErrorHandler: () => () => {},
};
