import React from "react";
import PropTypes from "prop-types";
import { Box, Button, Tooltip, ButtonGroup } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import { Team, TEAM_FIELD_STRUCTURE } from "./Team";
import TaskStatusIcon from "@mui/icons-material/Segment";
import TeamAchievementIcon from "@mui/icons-material/StarHalf";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import VisibilityIcon from "@mui/icons-material/Visibility";

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

export function TeamViewSettingsPanel({ mediaTypes, selectedMediaType, canShow, isSomethingSelected, canHide, isPossibleToHide,
    onShowTeam, onHideTeam, isTaskStatusShown, setIsTaskStatusShown, isTeamAchievementShown, setIsTeamAchievementShown }) {
    canShow = canShow ?? isSomethingSelected;
    canHide = canHide ?? isPossibleToHide;
    return (<ButtonGroup>
        {mediaTypes.map((elem) => (
            <Button
                disabled={!canShow}
                sx={{ ...gridButton,
                    backgroundColor: (selectedMediaType === elem.mediaType ? "#1976d2" : "primary")
                }}
                variant={selectedMediaType === elem.mediaType ? "contained" : "outlined"}
                key={elem.text}
                onClick={() => {onShowTeam(elem.mediaType);}}>{elem.text}</Button>
        ))}
        {isTaskStatusShown !== undefined && <CompactSwitchIconButton propertyName={"Tasks status"} disabled={!canShow}
            isShown={isTaskStatusShown} sx={gridButton}
            onClick={() => setIsTaskStatusShown(s => !s)}><TaskStatusIcon/></CompactSwitchIconButton>}
        {isTeamAchievementShown !== undefined && <CompactSwitchIconButton propertyName={"Team achievement"} disabled={!canShow}
            isShown={isTeamAchievementShown} buttonSx={gridButton}
            onClick={() => setIsTeamAchievementShown(s => !s)}><TeamAchievementIcon/></CompactSwitchIconButton>}
        <Button
            sx={gridButton}
            disabled={!canHide}
            variant={!canHide ? "outlined" : "contained"}
            color="error"
            onClick={() => onHideTeam()}>hide</Button>
    </ButtonGroup>);
}
TeamViewSettingsPanel.propTypes = {
    mediaTypes: PropTypes.arrayOf(PropTypes.shape({ "text":PropTypes.string.isRequired, "mediaType":PropTypes.any })),
    selectedMediaType: PropTypes.any,
    isSomethingSelected: PropTypes.bool,
    canShow: PropTypes.bool, // todo: make req
    isPossibleToHide: PropTypes.bool,
    canHide: PropTypes.bool,  // todo: make req
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
