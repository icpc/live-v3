import React from "react";

import "./App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import Button from "@mui/material/Button";


export default class ShowButton extends React.Component{
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.active === false) {
            return <Button onClick={this.props.onClick}><VisibilityOffIcon/></Button>;
        } else {
            return <Button onClick={this.props.onClick}><VisibilityIcon/></Button>;
        }
    }
}

