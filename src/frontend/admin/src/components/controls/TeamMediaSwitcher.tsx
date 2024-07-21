import { TeamMediaType } from "@shared/api";
import { Button, ButtonGroup } from "@mui/material";

export const DEFAULT_MEDIA_TYPES = [TeamMediaType.screen, TeamMediaType.camera, TeamMediaType.record, TeamMediaType.photo, null];

const mediaTypeName = (type: TeamMediaType | null) => {
    return type === null ? "empty" : type;
};

type TeamMediaSwitcherProps = {
    mediaTypes?: (TeamMediaType | null)[];
    disabledMediaTypes?: TeamMediaType[];
    switchedMediaType?: TeamMediaType | null;
    onSwitch: (type: TeamMediaType | null) => void
}

const TeamMediaSwitcher = ({ mediaTypes = DEFAULT_MEDIA_TYPES, disabledMediaTypes, switchedMediaType, onSwitch }: TeamMediaSwitcherProps) => {
    return (
        <ButtonGroup>
            {mediaTypes.map(t => (
                <Button
                    disabled={disabledMediaTypes?.includes(t)}
                    // color={secondaryMediaType === elem.mediaType ? "warning" : "primary"}
                    sx={{ mx: 2 }}
                    variant={t === switchedMediaType ? "contained" : "outlined"}
                    key={t}
                    onClick={() => onSwitch(t)}
                >
                    {mediaTypeName(t)}
                </Button>
            ))}
        </ButtonGroup>
    );
};

export default TeamMediaSwitcher;
