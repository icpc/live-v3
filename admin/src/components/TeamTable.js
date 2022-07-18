import React from "react";
import PropTypes from "prop-types";
import { Grid, Box, Button, TextField, Tooltip } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import { Team, TEAM_FIELD_STRUCTURE } from "./Team";
import { BASE_URL_BACKEND } from "../config";
import TaskStatusIcon from "@mui/icons-material/Segment";
import TeamAchievementIcon from "@mui/icons-material/StarHalf";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import VisibilityIcon from "@mui/icons-material/Visibility";
import { createApiPost } from "../utils";

const gridButton = {
    mx: "2px",
};

const CompactSwitchIconButton = ({ propertyName, disabled, isShown, onClick, children, sx }) =>
    (<Tooltip title={propertyName +" " + (isShown ? "will" : "wont") + " shown"}>
        <span><Button
            sx={sx}
            disabled={disabled}
            startIcon={isShown ? <VisibilityIcon/> : <VisibilityOffIcon/>}
            variant={isShown ? "contained" : "outlined"}
            onClick={onClick}>{children}</Button></span>
    </Tooltip>);
CompactSwitchIconButton.propTypes = {
    propertyName: PropTypes.string,
    disabled: PropTypes.bool.isRequired,
    isShown: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
    children: PropTypes.element,
    sx: PropTypes.object,
};

export function TeamViewSettingsPanel({ mediaTypes, selectedMediaType, isSomethingSelected, isPossibleToHide,
    onShowTeam, onHideTeam, isTaskStatusShown, setIsTaskStatusShown, isTeamAchievementShown, setIsTeamAchievementShown }) {
    return (<>
        {mediaTypes.map((elem) => (
            <Button
                disabled={!isSomethingSelected}
                sx={{ ...gridButton,
                    backgroundColor: (selectedMediaType === elem.mediaType ? "#1976d2" : "primary")
                }}
                variant={selectedMediaType === elem.mediaType ? "contained" : "outlined"}
                key={elem.text}
                onClick={() => {onShowTeam(elem.mediaType);}}>{elem.text}</Button>
        ))}
        {isTaskStatusShown !== undefined && <CompactSwitchIconButton propertyName={"Tasks status"} disabled={!isSomethingSelected}
            isShown={isTaskStatusShown} sx={gridButton}
            onClick={() => setIsTaskStatusShown(s => !s)}><TaskStatusIcon/></CompactSwitchIconButton>}
        {isTeamAchievementShown !== undefined && <CompactSwitchIconButton propertyName={"Team achievement"} disabled={!isSomethingSelected}
            isShown={isTeamAchievementShown} buttonSx={gridButton}
            onClick={() => setIsTeamAchievementShown(s => !s)}><TeamAchievementIcon/></CompactSwitchIconButton>}
        <Button
            sx={gridButton}
            disabled={!isPossibleToHide}
            variant={!isPossibleToHide ? "outlined" : "contained"}
            color="error"
            onClick={() => onHideTeam()}>hide</Button>
    </>);
}
TeamViewSettingsPanel.propTypes = {
    mediaTypes: PropTypes.arrayOf(PropTypes.shape({ "text":PropTypes.string.isRequired, "mediaType":PropTypes.any })).isRequired,
    selectedMediaType: PropTypes.any,
    isSomethingSelected: PropTypes.bool.isRequired,
    isPossibleToHide: PropTypes.bool.isRequired,
    onShowTeam: PropTypes.func.isRequired,
    onHideTeam: PropTypes.func.isRequired,
    isTaskStatusShown: PropTypes.bool,
    setIsTaskStatusShown: PropTypes.func,
    isTeamAchievementShown: PropTypes.bool,
    setIsTeamAchievementShown: PropTypes.func,
};
TeamViewSettingsPanel.defaultProps = {
    mediaTypes:[
        { text: "camera", mediaType: "camera" },
        { text: "screen", mediaType: "screen" },
        { text: "record", mediaType: "record" },
        { text: "info", mediaType: undefined },
    ]
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
        this.state = {
            dataElements: [],
            loaded: false,
            searchFieldValue: "",
            selectedId: undefined,
            shownId: undefined,
            shownMediaType: null,
        };
        this.apiPost = createApiPost(BASE_URL_BACKEND + this.props.apiPath);
        this.updateData = this.updateData.bind(this);
        this.showTeam = this.showTeam.bind(this);
        this.hideTeam = this.hideTeam.bind(this);
        this.apiUrl = this.apiUrl.bind(this);
        this.handleSearchFieldChange = this.handleSearchFieldChange.bind(this);
    }

    suppoertedMediaTypes() {
        return undefined;
    }

    apiUrl() {
        return BASE_URL_BACKEND + this.props.apiPath;
    }

    isTeamShown(stat, id) {
        return stat.shown && stat.settings.teamId === id;
    }

    updateStateByStatus(status, teamsData) {
        this.setState(state => ({ ...state,
            dataElements: teamsData,
            shownMediaType: (status.shown ? status.settings.mediaType : null),
            shownId: (status.shown ? status.settings.teamId : undefined),
            isTaskStatusShown: status.settings.showTaskStatus,
            isAchievementShown: status.settings.showAchievement,
        }));
    }

    updateData() {
        Promise.all([
            fetch(this.apiUrl())
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams")),
            fetch(this.apiUrl() + "/teams")
                .then(res => res.json())
                .catch(this.props.createErrorHandler("Failed to load list of teams"))
        ]).then(([status, response]) => {
            const teamsData = response.map((elem) => {
                elem.shown = this.isTeamShown(status, elem.id);
                elem.selected = false;
                return elem;
            });
            this.updateStateByStatus(status, teamsData);
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

    showTeam(mediaType = undefined) {
        this.apiPost("/show_with_settings", {
            teamId: this.state.selectedId,
            mediaType: mediaType,
            showTaskStatus: this.state.isTaskStatusShown,
            showAchievement: this.state.isAchievementShown,
        })
            .then(() => this.updateData())
            .then(() => this.setState({ ...this.state, selectedId: undefined }));
    }

    hideTeam() {
        this.apiPost("/hide").then(() => this.updateData()).then(() => this.setState({ ...this.stat, selectedId: undefined }));
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
                    <TeamViewSettingsPanel
                        mediaTypes={this.suppoertedMediaTypes()}
                        isPossibleToHide={this.state.shownId !== undefined}
                        isSomethingSelected={this.state.selectedId !== undefined}
                        selectedMediaType={this.state.shownMediaType} onShowTeam={this.showTeam} onHideTeam={this.hideTeam}
                        isTaskStatusShown={this.state.isTaskStatusShown}
                        setIsTaskStatusShown={() => this.setState(s => ({ ...s, isTaskStatusShown: !s.isTaskStatusShown }))}
                        isTeamAchievementShown={this.state.isAchievementShown}
                        setIsTeamAchievementShown={() => this.setState(s => ({ ...s, isAchievementShown: !s.isAchievementShown }))}/>
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
