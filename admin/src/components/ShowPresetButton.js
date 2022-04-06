import React from "react";

import "../App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import IconButton from "@mui/material/IconButton";

export default class ShowPresetButton extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.active) {
            return <IconButton color="primary" onClick={this.props.onClick}><VisibilityIcon/></IconButton>;
        } else {
            return <IconButton color="primary" onClick={this.props.onClick}><VisibilityOffIcon/></IconButton>;
        }
    }
}
