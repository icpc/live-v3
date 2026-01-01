import React from "react";
import styled, { keyframes, Keyframes } from "styled-components";
import { CornerContestantInfo } from "../../molecules/info/ContestantViewCorner";
import c from "../../../config";
import { Widget } from "@shared/api";
import { OverlayWidgetC } from "./types";

const slideIn = keyframes`
  from {
    opacity: 0.1;
  }
  to {
    opacity: 1;
  }
`;

const slideOut = keyframes`
  from {
    opacity: 1;
  }
  to {
    opacity: 0.1;
  }
`;

const TeamViewWrapper = styled.div<{
    top: number;
    left: number;
    animation: Keyframes;
    animationStyle: string;
    duration: number;
}>`
    position: absolute;
    top: ${({ top }) => top}px;
    left: ${({ left }) => left}px;
`;

const LineWrapper = styled.div<{
    animation: Keyframes;
    animationStyle: string;
    duration: number;
}>`
    width: 100%;
    height: 100%;
`;

interface Circle {
    x: number;
    y: number;
    radius: number;
    teamId: string;
}

export const Locator: OverlayWidgetC<Widget.TeamLocatorWidget> = ({
    widgetData,
    transitionState,
}) => {
    const circles: Circle[] = widgetData.settings.circles;
    const css = `
    .circles{filter:url(#maskfilter);}
    .circle{fill:#FFFFFF;}
    .backdrop{opacity:0.75;mask:url(#mask);}
    `;

    console.log(widgetData);
    return (
        <div className="mask2" style={{ zIndex: "-1" }}>
            <svg x="0px" y="0px" viewBox="0 0 1920 1080">
                <style type="text/css">{css}</style>
                <defs>
                    <filter
                        id="maskfilter"
                        filterUnits="userSpaceOnUse"
                        x="0"
                        y="0"
                        width="1920"
                        height="1080"
                    >
                        <feColorMatrix
                            type="matrix"
                            values="-1 0 0 0 1  0 -1 0 0 1  0 0 -1 0 1  0 0 0 1 0"
                            colorInterpolationFilters="sRGB"
                            result="source"
                        />
                        <feFlood
                            style={{ floodColor: "white", floodOpacity: "1" }}
                            result="back"
                        />
                        <feBlend in="source" in2="back" mode="normal" />
                    </filter>
                </defs>
                <mask
                    maskUnits="userSpaceOnUse"
                    x="0"
                    y="0"
                    width="1920"
                    height="1080"
                    id="mask"
                >
                    <g className="circles">
                        {circles.map((circle, index) => {
                            return (
                                <circle
                                    key={index + "circle"}
                                    className="circle"
                                    cx={circle.x}
                                    cy={circle.y}
                                    r={circle.radius}
                                />
                            );
                        })}
                    </g>
                </mask>
                <rect
                    x="0"
                    y="0"
                    className="backdrop"
                    height="100%"
                    width="100%"
                />
            </svg>
            {circles.map((circle, index) => {
                let left = circle.x - c.LOCATOR_MAGIC_CONSTANT / 2;
                let top;
                if (
                    circle.y - circle.radius - c.LOCATOR_TOP_OFFSET >
                    c.LOCATOR_TOP_THRESHOLD
                ) {
                    top = circle.y - circle.radius - c.LOCATOR_TOP_OFFSET;
                } else {
                    top = circle.y + circle.radius + c.LOCATOR_BOTTOM_OFFSET;
                }
                if (left < 0) {
                    left = 0;
                } else if (
                    left + c.LOCATOR_MAGIC_CONSTANT >
                    c.LOCATOR_MAX_WIDTH
                ) {
                    left = c.LOCATOR_MAX_WIDTH - c.LOCATOR_MAGIC_CONSTANT;
                }
                const len = Math.sqrt(
                    (left + c.LOCATOR_MAGIC_CONSTANT / 2 - circle.x) *
                        (left + c.LOCATOR_MAGIC_CONSTANT / 2 - circle.x) +
                        (top + c.LOCATOR_TOP_THRESHOLD - circle.y) *
                            (top + c.LOCATOR_TOP_THRESHOLD - circle.y),
                );
                return (
                    <div
                        style={{
                            position: "absolute",
                            top: 0,
                            left: 0,
                            width: "100%",
                            height: "100%",
                        }}
                        key={circle.teamId}
                    >
                        <TeamViewWrapper
                            top={top}
                            left={left}
                            animation={
                                transitionState === "exiting"
                                    ? slideOut
                                    : slideIn
                            }
                            animationStyle={
                                transitionState === "exiting"
                                    ? "ease-in"
                                    : "ease-out"
                            }
                            duration={
                                (index + 1) * c.LOCATOR_ANIMATION_DURATION
                            }
                        >
                            {/* FIXME: This needs readdressing for overlay2 */}
                            {/*<TeamInfo key={index + "teamInfo"} teamId={circle.teamId}/>*/}
                            <CornerContestantInfo teamId={circle.teamId} />
                        </TeamViewWrapper>

                        <LineWrapper
                            animation={
                                transitionState === "exiting"
                                    ? slideOut
                                    : slideIn
                            }
                            animationStyle={
                                transitionState === "exiting"
                                    ? "ease-in"
                                    : "ease-out"
                            }
                            duration={
                                (index + 1) * c.LOCATOR_ANIMATION_DURATION -
                                c.LOCATOR_ANIMATION_DELAY
                            }
                        >
                            <svg
                                key={index + "path"}
                                height="100%"
                                width="100%"
                                stroke={c.LOCATOR_LINE_STROKE_COLOR}
                                strokeWidth={c.LOCATOR_LINE_STROKE_WIDTH}
                                fill="none"
                            >
                                <path
                                    d={`M ${circle.x + ((left + c.LOCATOR_MAGIC_CONSTANT / 2 - circle.x) / len) * circle.radius} ${circle.y + ((top + c.LOCATOR_TOP_THRESHOLD - circle.y) / len) * circle.radius} L ${left + c.LOCATOR_MAGIC_CONSTANT / 2} ${top + c.LOCATOR_TOP_THRESHOLD}`}
                                />
                            </svg>
                        </LineWrapper>
                    </div>
                );
            })}
        </div>
    );
};

export default Locator;
