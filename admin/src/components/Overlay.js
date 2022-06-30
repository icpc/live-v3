import { Paper } from "@mui/material";
import React, { Component } from "react";
import { OVERLAY_LOCATION } from "../config";
import { Rnd } from "react-rnd";

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
    onResize(e, direction, ref) {
        this.setState({ ...this.state, scaleFactor: ref.offsetWidth / FULL_WIDTH });
    }
    render() {
        return <Rnd
            width={FULL_WIDTH * this.state.scaleFactor}
            height={FULL_HEIGHT * this.state.scaleFactor}
            onResize={this.onResize}
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
        </Rnd>;
    }
}
