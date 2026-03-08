import { Paper } from "@mui/material";
import React, { useCallback } from "react";
import { OVERLAY_LOCATION } from "../config";
import { Rnd } from "react-rnd";
import { useLocalStorageState } from "../utils";

const FULL_WIDTH = 1920;
const FULL_HEIGHT = 1080;

type OverlayState = {
    scaleFactor: number;
    offsetX: number;
    offsetY: number;
};

type OverlayProps = {
    isOverlayPreviewShown: boolean;
};

export function Overlay({
    isOverlayPreviewShown,
}: OverlayProps): React.ReactElement {
    const [state, setState] = useLocalStorageState<OverlayState>(
        "OverlayPreviewPosition",
        {
            scaleFactor: 0.3,
            offsetX: 0,
            offsetY: 0,
        },
    );

    type RndProps = React.ComponentProps<typeof Rnd>;

    const onResize = useCallback<NonNullable<RndProps["onResize"]>>(
        (_e, _direction, ref) =>
            setState({
                ...state,
                scaleFactor: ref.offsetWidth / FULL_WIDTH,
            }),
        [setState],
    );

    const onDrag = useCallback<NonNullable<RndProps["onDrag"]>>(
        (_e, ref) =>
            setState({
                ...state,
                offsetX: ref.lastX,
                offsetY: ref.lastY,
            }),
        [setState],
    );

    if (!isOverlayPreviewShown) return null;

    const scaledWidth = FULL_WIDTH * state.scaleFactor;
    const scaledHeight = FULL_HEIGHT * state.scaleFactor;

    return (
        <Rnd
            position={{ x: state.offsetX, y: state.offsetY }}
            size={{ width: scaledWidth, height: scaledHeight }}
            onResize={onResize}
            onDrag={onDrag}
            lockAspectRatio
            bounds="body"
        >
            <Paper
                sx={{
                    overflow: "hidden",
                    width: scaledWidth,
                    height: scaledHeight,
                }}
            >
                <iframe
                    src={OVERLAY_LOCATION}
                    width={FULL_WIDTH}
                    height={FULL_HEIGHT}
                    style={{
                        transform: `scale(${state.scaleFactor})`,
                        transformOrigin: "top left",
                        pointerEvents: "none",
                        border: "none",
                    }}
                />
            </Paper>
        </Rnd>
    );
}
