import React from "react";

import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { Button } from "@mui/material";
import PropTypes from "prop-types";

export default function ShowPresetButton({ active, onClick }) {
    return (
        <Button
            color={active ? "error" : "primary"}
            startIcon={active ? <VisibilityOffIcon/> : <VisibilityIcon/>}
            onClick={onClick}
            sx={{ width: "100px" }}>
            {active ? "Hide" : "Show"}
        </Button>
    );
}

ShowPresetButton.propTypes = {
    active: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
};
