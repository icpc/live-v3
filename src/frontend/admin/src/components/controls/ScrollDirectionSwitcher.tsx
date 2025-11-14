import { Button, ButtonGroup, SvgIconTypeMap } from "@mui/material";
import SkipPreviousIcon from "@mui/icons-material/SkipPrevious";
import PauseIcon from "@mui/icons-material/Pause";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SkipNextIcon from "@mui/icons-material/SkipNext";
import { OverridableComponent } from "@mui/material/OverridableComponent";
import { ScoreboardScrollDirection } from "@shared/api";

export type ScrollDirectionSwitcherProps = {
    direction: ScoreboardScrollDirection;
    setDirection: (duration: ScoreboardScrollDirection) => void;
};

const availableDirections: [
    ScoreboardScrollDirection,
    OverridableComponent<SvgIconTypeMap>,
][] = [
    [ScoreboardScrollDirection.FirstPage, SkipPreviousIcon],
    [ScoreboardScrollDirection.Back, PlayArrowIcon],
    [ScoreboardScrollDirection.Pause, PauseIcon],
    [ScoreboardScrollDirection.Forward, PlayArrowIcon],
    [ScoreboardScrollDirection.LastPage, SkipNextIcon],
];

const ScrollDirectionSwitcher = ({
    direction,
    setDirection,
}: ScrollDirectionSwitcherProps) => {
    return (
        <ButtonGroup variant="outlined">
            {availableDirections.map(([v, C]) => (
                <Button
                    color={direction == v ? "error" : "primary"}
                    variant={direction == v ? "contained" : "outlined"}
                    onClick={() => setDirection(v)}
                    key={v}
                >
                    <C
                        sx={{
                            transform:
                                v == ScoreboardScrollDirection.Back
                                    ? "rotate(180deg)"
                                    : undefined,
                        }}
                    />
                </Button>
            ))}
        </ButtonGroup>
    );
};

export default ScrollDirectionSwitcher;
