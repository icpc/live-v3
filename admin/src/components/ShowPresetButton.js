import React from "react";

import "../App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { Button } from "@mui/material";
import PropTypes from "prop-types";

export default class ShowPresetButton extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <Button
            color={this.props.active ? "error" : "primary"}
            startIcon={this.props.active ? <VisibilityOffIcon/> : <VisibilityIcon/>}
            onClick={this.props.onClick}
            sx={{ width: "100px" }}
        >
            {this.props.active ? "Hide" : "Show"}
        </Button>;
        // uncomment this part if you ever want an old version of show button
        // if (this.props.active) {
        // return <Button color="primary" disabled={this.props.active} onClick={this.props.onClick}>Show</Button>;
        // return <IconButton color="primary" onClick={this.props.onClick}><VisibilityIcon/> SHOW</IconButton>;
        // } else {
        // return <Button color="error" disabled={!this.props.active} onClick={this.props.onClick}>Hide</Button>;
        // return <IconButton color="primary" onClick={this.props.onClick}><VisibilityOffIcon/> SHOW</IconButton>;
        // }
    }
}


ShowPresetButton.propTypes = {
    active: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
};
