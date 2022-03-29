import React from "react";

import "./App.css";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import Button from "@mui/material/Button";


export default class ShowButton extends React.Component{
    constructor(props) {
        super(props);

        this.state = {
            visible: false
        };

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(ev) {
        if (this.state.visible == false) {
            this.setState({ visible: true });
        } else {
            this.setState({ visible: false });
        }
        this.props.onClick(ev);
    }

    render() {
        if (this.state.visible == false) {
            return <Button onClick={this.handleClick}><VisibilityIcon/></Button>;
        } else {
            return <Button onClick={this.handleClick}><VisibilityOffIcon/></Button>;
        }
    }
}

