import React, { useCallback, useMemo, useState } from "react";
import { Box, Button, Tooltip, ButtonGroup, alpha } from "@mui/material";
import { lightBlue, grey } from "@mui/material/colors";
import CollectionsIcon from "@mui/icons-material/Collections";
import TaskStatusIcon from "@mui/icons-material/Segment";
import TeamAchievementIcon from "@mui/icons-material/StarHalf";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import VisibilityIcon from "@mui/icons-material/Visibility";
import AutoModeIcon from "@mui/icons-material/AutoMode";
import Typography from "@mui/material/Typography";
import {
    TeamId,
    TeamMediaType,
    MediaType as ApiMediaType,
    WidgetUsageStatisticsEntry
} from "@shared/api";


export interface TeamViewData {
    id: TeamId | null;
    shown: boolean;
    selected: boolean;
    shortName: string;
    medias: Partial<Record<TeamMediaType, ApiMediaType>>;
}

export interface MediaTypeOption {
    text: string;
    mediaType: TeamMediaType | null;
}

export interface UsageStats {
    byTeam: Record<TeamId, WidgetUsageStatisticsEntry>;
}


export interface TableStyle {
    activeColor: string;
    inactiveColor: string;
    selectedColor: string;
}

export interface TeamRowProps {
    team: TeamViewData;
    style: TableStyle;
    usageStats?: UsageStats;
    onClick: (teamId: TeamId | null) => void;
}



export interface ToggleButtonProps {
    label: string;
    isActive: boolean;
    disabled?: boolean;
    onClick: () => void;
    icon?: React.ReactNode;
    showVisibilityIcon?: boolean;
    color?: "primary" | "warning" | "error";
    variant?: "contained" | "outlined";
}

export interface MediaButtonsProps {
    mediaTypes: MediaTypeOption[];
    selectedMediaTypes: (TeamMediaType | null)[];
    secondaryMediaType?: TeamMediaType | null;
    disabled?: boolean;
    onMediaSelect: (mediaType: TeamMediaType | null) => void;
}
export interface SettingsPanelProps {
    mediaTypes?: MediaTypeOption[];
    selectedMediaTypes: (TeamMediaType | null)[];
    canShow?: boolean;
    canHide?: boolean;
    showHideButton?: boolean;
    enableMultipleMode?: boolean;
    onShowTeam: (mediaTypes: (TeamMediaType | null)[]) => void;
    onHideTeam?: () => void;
    // Status settings
    isStatusShown?: boolean;
    onStatusToggle?: (shown: boolean) => void;
    // Achievement settings
    isAchievementShown?: boolean;
    onAchievementToggle?: (shown: boolean) => void;
}

export interface TeamTableProps {
    teams: TeamViewData[];
    onTeamClick: (teamId: TeamId | null) => void;
    style?: TableStyle;
    usageStats?: UsageStats;
    RowComponent?: React.ComponentType<TeamRowProps>;
}


const DEFAULT_MEDIA_TYPES: MediaTypeOption[] = [
    { text: "Camera", mediaType: TeamMediaType.camera },
    { text: "Screen", mediaType: TeamMediaType.screen },
    { text: "Record", mediaType: TeamMediaType.record },
    { text: "Photo", mediaType: TeamMediaType.photo },
    { text: "Empty", mediaType: null },
];

const DEFAULT_TABLE_STYLE: TableStyle = {
    selectedColor: grey.A200,
    activeColor: lightBlue[100],
    inactiveColor: "#fff",
};

function formatUsageTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${remainingSeconds.toString().padStart(2, "0")}`;
}

function getUsageTimeFromStats(teamId: TeamId, usageStats?: UsageStats): number | undefined {
    if (!teamId || !usageStats?.byTeam[teamId]) return undefined;

    const stats = usageStats.byTeam[teamId];
    if (stats.type === "simple") {
        return stats.totalShownTimeSeconds;
    }

    return undefined;
};

function getTeamBackgroundColor(team: TeamViewData, style: TableStyle): string {
    if (team.shown) return style.activeColor;
    if (team.selected) return style.selectedColor;
    return style.inactiveColor;
}

function getTeamTextColor(team: TeamViewData): string {
    return team.selected || team.shown ? grey[900] : grey[700];
}

function isMediaTypeSelected(
    mediaType: TeamMediaType | null,
    selectedTypes: (TeamMediaType | null)[],
): boolean {
    if (mediaType === null) {
        return selectedTypes.length === 0;
    }

    return selectedTypes.includes(mediaType);
}

function ToggleButton({
    label,
    isActive,
    disabled = false,
    onClick,
    icon,
    showVisibilityIcon = true,
    color = "primary",
    variant,
}: ToggleButtonProps): React.ReactElement {
    const buttonVariant = variant || (isActive ? "contained" : "outlined");
    const visibilityIcon = showVisibilityIcon
        ? isActive
            ? <VisibilityIcon />
            : <VisibilityOffIcon />
        : undefined;

    return (
        <Tooltip title={`${label} ${isActive ? "will" : "won't"} be shown`}>
            <Button
                disabled={disabled}
                startIcon={visibilityIcon || icon}
                variant={buttonVariant}
                color={color}
                onClick={onClick}
                sx={{ mx: 0.25 }}
            >
                {icon && !showVisibilityIcon ? icon : null}
            </Button>
        </Tooltip>
    );
}

function MediaButtons({
    mediaTypes,
    selectedMediaTypes,
    secondaryMediaType,
    disabled = false,
    onMediaSelect,
}: MediaButtonsProps): React.ReactElement {
    return (
        <>
            {mediaTypes.map((mediaOption) => {
                const isSelected = isMediaTypeSelected(mediaOption.mediaType, selectedMediaTypes);
                const isSecondary = secondaryMediaType === mediaOption.mediaType;
                const color = isSecondary ? "warning" : "primary";
                const variant = isSelected || isSecondary ? "contained" : "outlined";

                return (
                    <Button
                        key={mediaOption.text}
                        disabled={disabled}
                        color={color}
                        variant={variant}
                        onClick={() => onMediaSelect(mediaOption.mediaType)}
                        sx={{ mx: 0.25 }}
                    >
                        {mediaOption.text}
                    </Button>
                );
            })}
        </>
    );
}

function TeamRow({
    team,
    style,
    usageStats,
    onClick
}: TeamRowProps): React.ReactElement {
    const backgroundColor = getTeamBackgroundColor(team, style);
    const textColor = getTeamTextColor(team);

    const usageTime = team.id ? getUsageTimeFromStats(team.id, usageStats) : undefined;


    return (
        <Box
            onClick={() => onClick(team.id)}
            sx={{
                backgroundColor,
                color: textColor,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                width: "100%",
                height: "100%",
                minHeight: 48,
                cursor: "pointer",
                margin: 0.5,
                padding: 1,
                borderRadius: 1,
                borderBottom: `1px solid ${alpha(grey[400], 0.3)}`,
                transition: "all 0.2s ease-in-out",
                "&:hover": {
                    backgroundColor: alpha(backgroundColor, 0.8),
                    transform: "translateY(-1px)",
                    boxShadow: 1,
                },
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center" }}>
                {team.id === null && <AutoModeIcon sx={{ mr: 1 }} />}
                <Typography variant="body2" component="span">
                    {team.id && `${team.id}: `}
                    {team.shortName}
                </Typography>
            </Box>
            {usageTime && (
                <Typography variant="caption" color="text.secondary">
                    {formatUsageTime(usageTime)}
                </Typography>
            )}
        </Box>
    );
}

export function TeamSettingsPanel({
    mediaTypes = DEFAULT_MEDIA_TYPES,
    selectedMediaTypes,
    canShow = false,
    canHide = false,
    showHideButton = true,
    enableMultipleMode = false,
    onShowTeam,
    onHideTeam,
    isStatusShown,
    onStatusToggle,
    isAchievementShown,
    onAchievementToggle,
}: SettingsPanelProps): React.ReactElement {
    const [isMultipleMode, setIsMultipleMode] = useState(false);
    const [secondaryMediaType, setSecondaryMediaType] = useState<TeamMediaType | null | undefined>(undefined);

    const handleMultipleModeToggle = useCallback(() => {
        setIsMultipleMode((prev) => !prev);
        setSecondaryMediaType(undefined);
    }, []);

    const handleMediaSelect = useCallback(
        (mediaType: TeamMediaType | null) => {
            if (!isMultipleMode) {
                const mediaTypes = mediaType === null ? [] : [mediaType];
                onShowTeam(mediaTypes);
                return;
            }

            if (secondaryMediaType === undefined) {
                setSecondaryMediaType(mediaType);
            } else if (secondaryMediaType === mediaType) {
                setSecondaryMediaType(undefined);
            } else {
                const mediaTypes = [secondaryMediaType, mediaType].filter(
                    (type): type is TeamMediaType => type !== null
                );
                onShowTeam(mediaTypes);
                setSecondaryMediaType(undefined);
            }
        },
        [isMultipleMode, secondaryMediaType, onShowTeam]
    );

    return (
        <ButtonGroup variant="outlined" sx={{ flexWrap: "wrap", gap: 0.5 }}>
            {enableMultipleMode && (
                <ToggleButton
                    label="Multiple mode"
                    isActive={isMultipleMode}
                    disabled={!canShow}
                    onClick={handleMultipleModeToggle}
                    icon={<CollectionsIcon />}
                    showVisibilityIcon={false}
                />
            )}

            <MediaButtons
                mediaTypes={mediaTypes}
                selectedMediaTypes={selectedMediaTypes}
                secondaryMediaType={secondaryMediaType}
                disabled={!canShow}
                onMediaSelect={handleMediaSelect}
            />

            {onStatusToggle && isStatusShown !== undefined && (
                <ToggleButton
                    label="Task status"
                    isActive={isStatusShown}
                    disabled={!canShow}
                    onClick={() => onStatusToggle(!isStatusShown)}
                    icon={<TaskStatusIcon />}
                />
            )}

            {onAchievementToggle && isAchievementShown !== undefined && (
                <ToggleButton
                    label="Team achievement"
                    isActive={isAchievementShown}
                    disabled={!canShow}
                    onClick={() => onAchievementToggle(!isAchievementShown)}
                    icon={<TeamAchievementIcon />}
                />
            )}

            {showHideButton && onHideTeam && (
                <Button
                    disabled={!canHide}
                    variant={canHide ? "contained" : "outlined"}
                    color="error"
                    onClick={onHideTeam}
                    sx={{ mx: 0.25 }}
                >
                    Hide
                </Button>
            )}
        </ButtonGroup>
    );
}

export function TeamTable({
    teams,
    onTeamClick,
    style = DEFAULT_TABLE_STYLE,
    usageStats,
    RowComponent = TeamRow,
}: TeamTableProps): React.ReactElement {
    const gridColumns = useMemo(
        () => ({
            xs: "repeat(1, 1fr)",
            sm: "repeat(2, 1fr)",
            md: "repeat(4, 1fr)",
        }),
        []
    );

    return (
        <Box
            sx={{
                display: "grid",
                gridTemplateColumns: gridColumns,
                gap: 1,
                padding: 1,
            }}
        >
            {teams.map((team) => (
                <RowComponent
                    key={team.id || "auto"}
                    team={team}
                    style={style}
                    usageStats={usageStats}
                    onClick={onTeamClick}
                />
            ))}
        </Box>
    );
}
