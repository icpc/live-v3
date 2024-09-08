import React, { useState } from "react";
import PropTypes from "prop-types";
import { Box, Button, Tooltip, ButtonGroup, Grid } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import CollectionsIcon from "@mui/icons-material/Collections";
import TaskStatusIcon from "@mui/icons-material/Segment";
import TeamAchievementIcon from "@mui/icons-material/StarHalf";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import VisibilityIcon from "@mui/icons-material/Visibility";
import AutoModeIcon from "@mui/icons-material/AutoMode";

const gridButton = {
    mx: "2px",
};

export const TEAM_FIELD_STRUCTURE = PropTypes.shape({
    id: PropTypes.string,
    shown: PropTypes.bool.isRequired,
    selected: PropTypes.bool.isRequired,
    shortName: PropTypes.string.isRequired,
    medias: PropTypes.shape({
        screen: PropTypes.object,
        camera: PropTypes.object,
    }).isRequired,
});

const TeamTableRow = ({ rowData, onClick, tStyle }) => {
    return (<Grid sx={{ display: "flex", width: "100%", height: "100%" }}>
        <Box
            key={rowData.id}
            sx={{
                backgroundColor:
                    (rowData.shown?
                        tStyle.activeColor :
                        (rowData.selected?
                            tStyle.selectedColor :
                            tStyle.inactiveColor)),
                display: "flex",
                alignItems: "center",
                width: "100%",
                height: "100%",
                cursor: "pointer",
                margin: "4px",
                borderBottom: "1px solid rgba(224, 224, 224, 1)",
                color: (rowData.selected || rowData.shown ? grey[900] : grey[700]) }}
            onClick={() => onClick(rowData.id)}
        >
            {rowData.id && `${rowData.id} :`}
            {rowData.id === null && <AutoModeIcon sx={{ mr: 1 }} />}
            {" " + rowData.shortName}
        </Box>
    </Grid>);
};

TeamTableRow.propTypes = {
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
        selectedColor: PropTypes.string,
    }).isRequired,
    rowData: TEAM_FIELD_STRUCTURE,
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
    onClick: PropTypes.func.isRequired
};

const CompactSwitchIconButton = ({ propertyName, disabled, isShown, onClick, children, sx, noVisibilityIcon }) =>
    (<Tooltip title={propertyName +" " + (isShown ? "will" : "wont") + " shown"}>
        <Button
            sx={sx}
            disabled={disabled}
            startIcon={noVisibilityIcon ? undefined : (isShown ? <VisibilityIcon/> : <VisibilityOffIcon/>)}
            variant={isShown ? "contained" : "outlined"}
            onClick={onClick}>{children}</Button>
    </Tooltip>);
CompactSwitchIconButton.propTypes = {
    propertyName: PropTypes.string,
    disabled: PropTypes.bool.isRequired,
    isShown: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
    children: PropTypes.element,
    sx: PropTypes.object,
    noVisibilityIcon: PropTypes.bool,
};

const isMediaTypeChoosen = (mediaType, selectedMediaTypes) => {
    if (!selectedMediaTypes) {
        return false;
    }
    return selectedMediaTypes.includes(mediaType) || mediaType === null && selectedMediaTypes.length === 0;
};

// @deprecate Too hard
export const TeamViewSettingsPanel = ({
    mediaTypes,
    selectedMediaTypes,
    canShow,
    isSomethingSelected,
    canHide,
    isPossibleToHide,
    showHideButton,
    onShowTeam,
    onHideTeam,
    isStatusShown,
    setIsStatusShown,
    isAchievementShown,
    setIsAchievementShown,
    offerMultiple
}) => {
    canShow = canShow ?? isSomethingSelected;
    canHide = canHide ?? isPossibleToHide;
    showHideButton = showHideButton ?? true;
    const [isMultipleMode, setIsMultipleMode] = useState(false);
    const [secondaryMediaType, setSecondaryMediaType] = useState(undefined);
    const onShow = (mediaType) => {
        if (!isMultipleMode) {
            onShowTeam([mediaType].filter(t => t !== null));
        } else if (secondaryMediaType === undefined) {
            setSecondaryMediaType(mediaType);
        } else if (secondaryMediaType === mediaType) {
            setSecondaryMediaType(undefined);
        } else{
            onShowTeam([secondaryMediaType, mediaType].filter(t => t !== null));
            setSecondaryMediaType(undefined);
        }
    };
    return (<ButtonGroup>
        {offerMultiple && <CompactSwitchIconButton isShown={isMultipleMode} propertyName={"Multiple mode"}
            disabled={!canShow} sx={gridButton} noVisibilityIcon
            onClick={() => setIsMultipleMode(s => !s)}><CollectionsIcon/></CompactSwitchIconButton>}
        {mediaTypes.map((elem) => (
            <Button
                disabled={!canShow}
                color={secondaryMediaType === elem.mediaType ? "warning" : "primary"}
                sx={gridButton}
                variant={(isMediaTypeChoosen(elem.mediaType, selectedMediaTypes) || secondaryMediaType === elem.mediaType)
                    ? "contained" : "outlined"}
                key={elem.text}
                onClick={() => onShow(elem.mediaType)}>{elem.text}</Button>
        ))}
        {isStatusShown !== undefined && <CompactSwitchIconButton propertyName={"Tasks status"} disabled={!canShow}
            isShown={isStatusShown} sx={gridButton}
            onClick={() => setIsStatusShown(s => !s)}><TaskStatusIcon/></CompactSwitchIconButton>}
        {isAchievementShown !== undefined && <CompactSwitchIconButton propertyName={"Team achievement"} disabled={!canShow}
            isShown={isAchievementShown} buttonSx={gridButton}
            onClick={() => setIsAchievementShown(s => !s)}><TeamAchievementIcon/></CompactSwitchIconButton>}
        {showHideButton && (
            <Button
                sx={gridButton}
                disabled={!canHide}
                variant={!canHide ? "outlined" : "contained"}
                color="error"
                onClick={() => onHideTeam()}
            >
                hide
            </Button>
        )}
    </ButtonGroup>);
};

TeamViewSettingsPanel.propTypes = {
    mediaTypes: PropTypes.arrayOf(PropTypes.shape({ "text":PropTypes.string.isRequired, "mediaType":PropTypes.any })),
    selectedMediaTypes: PropTypes.arrayOf(PropTypes.string),
    isSomethingSelected: PropTypes.bool,
    canShow: PropTypes.bool, // todo: make req
    isPossibleToHide: PropTypes.bool,
    showHideButton: PropTypes.bool,
    canHide: PropTypes.bool,  // todo: make req
    onShowTeam: PropTypes.func.isRequired,
    onHideTeam: PropTypes.func,
    isStatusShown: PropTypes.bool,
    setIsStatusShown: PropTypes.func,
    isAchievementShown: PropTypes.bool,
    setIsAchievementShown: PropTypes.func,
    offerMultiple: PropTypes.bool,
};
TeamViewSettingsPanel.defaultProps = {
    mediaTypes:[
        { text: "camera", mediaType: "camera" },
        { text: "screen", mediaType: "screen" },
        { text: "record", mediaType: "record" },
        { text: "photo", mediaType: "photo" },
        { text: "empty", mediaType: null },
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
    RowComponent: TeamTableRow,
};
