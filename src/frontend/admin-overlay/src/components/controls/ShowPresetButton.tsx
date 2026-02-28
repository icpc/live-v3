import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { Button, ButtonProps } from "@mui/material";

type ShowPresetButtonProps = {
    checked: boolean;
    onClick: (newState: boolean) => void;
    disabled?: boolean;
    sx?: ButtonProps["sx"];
};

const ShowPresetButton = ({
    checked,
    onClick,
    disabled,
    sx = {},
}: ShowPresetButtonProps) => {
    return (
        <Button
            color={checked ? "error" : "primary"}
            startIcon={checked ? <VisibilityOffIcon /> : <VisibilityIcon />}
            onClick={(event) => {
                onClick(!checked);
                event.stopPropagation();
            }}
            disabled={disabled}
            sx={{ width: "100px", ...sx }}
        >
            {checked ? "Hide" : "Show"}
        </Button>
    );
};

export default ShowPresetButton;
