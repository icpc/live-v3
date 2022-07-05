import { Paper } from "@mui/material";
import React, { Component } from "react";
import { OVERLAY_LOCATION } from "../config";
import { Rnd } from "react-rnd";
import PropTypes from "prop-types";

const FULL_WIDTH = 1920;
const FULL_HEIGHT = 1080;

export class Overlay extends Component {
    constructor(props) {
        super(props);
        this.state = {
            scaleFactor: 0.3,
            offsetX: 0,
            offsetY: 0
        };
        this.onResize = this.onResize.bind(this);
        this.onDrag = this.onDrag.bind(this);
    }
    onResize(e, direction, ref) {
        this.setState(state =>({ ...state, scaleFactor: ref.offsetWidth / FULL_WIDTH }));
    }
    onDrag(e, ref) {
        this.setState(state =>({ ...state, offsetX: ref.lastX, offsetY: ref.lastY }));
    }
    render() {
        return this.props.isOverlayPreviewShown && (<Rnd
            position={{ x: this.state.offsetX, y: this.state.offsetY }}
            width={FULL_WIDTH * this.state.scaleFactor}
            height={FULL_HEIGHT * this.state.scaleFactor}
            onResize={this.onResize}
            onDrag={this.onDrag}
            lockAspectRatio={true}
            bounds={"body"}
        >
            <Paper sx={{
                overflow: "hidden",
                width: FULL_WIDTH * this.state.scaleFactor,
                height: FULL_HEIGHT * this.state.scaleFactor
            }}>
                <iframe src={OVERLAY_LOCATION} width={FULL_WIDTH} height={FULL_HEIGHT} style={{
                    transform: `scale(${this.state.scaleFactor})`,
                    transformOrigin: "top left",
                    pointerEvents: "none"
                }}/>
            </Paper>
        </Rnd>);
    }
}
Overlay.propTypes = {
    isOverlayPreviewShown: PropTypes.bool.isRequired,
};
