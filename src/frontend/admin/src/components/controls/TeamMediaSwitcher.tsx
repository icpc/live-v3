import { TeamMediaType } from "@shared/api";
import { Button } from "@mui/material";
import ButtonGroup from "@/components/atoms/ButtonGroup.tsx";

export const DEFAULT_MEDIA_TYPES = [TeamMediaType.screen, TeamMediaType.camera, TeamMediaType.record, TeamMediaType.photo, null];

const mediaTypeName = (type: TeamMediaType | null) => {
    return type === null ? "empty" : type;
};

type TeamMediaSwitcherProps = {
    mediaTypes?: (TeamMediaType | null)[];
    disabled?: boolean;
    disabledMediaTypes?: TeamMediaType[];
    switchedMediaType?: TeamMediaType | null;
    onSwitch: (type: TeamMediaType | null) => void;
    onSwitchHide?: () => void;
    disabledHide?: boolean;
}

const TeamMediaSwitcher = ({
    mediaTypes = DEFAULT_MEDIA_TYPES,
    disabled,
    disabledMediaTypes,
    switchedMediaType,
    onSwitch,
    onSwitchHide,
    disabledHide
}: TeamMediaSwitcherProps) => {
    return (
        <ButtonGroup>
            {mediaTypes.map(t => (
                <Button
                    disabled={disabled || disabledMediaTypes?.includes(t)}
                    // color={secondaryMediaType === elem.mediaType ? "warning" : "primary"}
                    sx={{ mx: 2 }}
                    variant={t === switchedMediaType ? "contained" : "outlined"}
                    key={t}
                    onClick={() => onSwitch(t)}
                >
                    {mediaTypeName(t)}
                </Button>
            ))}
            {onSwitchHide && (
                <Button
                    // sx={gridButton}
                    disabled={disabledHide}
                    variant={!disabledHide ? "outlined" : "contained"}
                    color="error"
                    onClick={() => onSwitchHide()}
                >
                    hide
                </Button>
            )}
        </ButtonGroup>
    );
};

export default TeamMediaSwitcher;
