import { Paper } from "@mui/material";
import React, { Component } from "react";
import { OVERLAY_LOCATION } from "../config";
import { Resizable } from "react-resizable";
import "react-resizable/css/styles.css";

const FULL_WIDTH = 1920;
const FULL_HEIGHT = 1080;

export class Overlay extends Component {
    constructor() {
        super();
        this.state = {
            scaleFactor: 0.3,
        };
        this.onResize = this.onResize.bind(this);
    }
    onResize(event, { size }) {
        this.setState({ ...this.state, scaleFactor: size.width / FULL_WIDTH });
    }
    render() {
        return <Resizable
            width={FULL_WIDTH * this.state.scaleFactor}
            height={FULL_HEIGHT * this.state.scaleFactor}
            onResize={this.onResize}
            resizeHandles={["nw", "n", "w"]}
            lockAspectRatio={true}
        >
            <Paper sx={{
                position: "fixed",
                bottom: "10px",
                right: "10px",
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
        </Resizable>;
    }
}
