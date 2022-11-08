import React from "react";
import { TeamInfo } from "../holder/TeamVeiwHolder";
import styled, { keyframes } from "styled-components";
import { TEAM_VIEW_APPEAR_TIME } from "../../../config";

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

const TeamViewWrapper = styled.div`
  position: absolute;
  top: ${({ top }) => top}px;
  left: ${({ left }) => left}px;
  animation:  ${TEAM_VIEW_APPEAR_TIME}ms ${props => props.animation} ${({ duration }) => duration}ms ${props => props.animationStyle};
    
`;

const LineWrapper = styled.div`
  width: 100%;
  height: 100%;
  animation-delay: ${TEAM_VIEW_APPEAR_TIME}ms;
  animation: ${TEAM_VIEW_APPEAR_TIME}ms ${props => props.animation} ${({ duration }) => duration}ms ${props => props.animationStyle};
    
`;

export const Locator = ({ widgetData, transitionState }) => {
    let circles = widgetData.settings.circles;
    const css = `
    .circles{filter:url(#maskfilter);}
    .circle{fill:#FFFFFF;}
    .backdrop{opacity:0.5;mask:url(#mask);}
    `;


    console.log(widgetData);
    return <div className="mask2">
        <svg x="0px" y="0px" viewBox="0 0 1920 1080">
            <style type="text/css">{css}</style>
            <defs>
                <filter id="maskfilter" filterUnits="userSpaceOnUse" x="0" y="0" width="1920" height="1080">
                    <feColorMatrix type="matrix" values="-1 0 0 0 1  0 -1 0 0 1  0 0 -1 0 1  0 0 0 1 0"
                        colorInterpolationFilters="sRGB" result="source"/>
                    <feFlood style={{ floodColor: "white", floodOpacity: "1" }} result="back"/>
                    <feBlend in="source" in2="back" mode="normal"/>
                </filter>
            </defs>
            <mask maskUnits="userSpaceOnUse" x="0" y="0" width="1920" height="1080" id="mask">
                <g className="circles">
                    {circles.map((circle, index) => {
                        return <circle key={index + "circle"} className="circle" cx={circle.x} cy={circle.y}
                            r={circle.radius}/>;
                    })
                    }
                </g>
            </mask>
            <rect x="0" y="0" className="backdrop" height="100%" width="100%"/>
        </svg>
        {circles.map((circle, index) => {
            let left = circle.x - 540 / 2;
            let top;
            if (circle.y - circle.radius - 50 > 10) {
                top = circle.y - circle.radius - 50;
            } else {
                top = circle.y + circle.radius + 16;
            }
            if (left < 0) {
                left = 0;
            } else if (left + 540 > 1920) {
                left = 1920 - 540;
            }
            let len = Math.sqrt((left + 540 / 2 - circle.x) * (left + 540 / 2 - circle.x) +
                (top + 10 - circle.y) * (top + 10 - circle.y));
            return <div style={{ position:"absolute", top:0, left:0, width:"100%", height:"100%" }} key={circle.teamId}>
                <TeamViewWrapper
                    top={top}
                    left={left}
                    animation={transitionState === "exiting" ? slideOut : slideIn}
                    animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
                    duration={(index + 1) * 1500}>
                    <TeamInfo key={index + "teamInfo"} teamId={circle.teamId}/>

                </TeamViewWrapper>

                <LineWrapper animation={transitionState === "exiting" ? slideOut : slideIn}
                    animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
                    duration={(index + 1) * 1500 - 500}>
                    <svg key={index + "path"} height="100%" width="100%" stroke="white" strokeWidth="5" fill="none">
                        <path d={`M ${circle.x + (left + 540 / 2 - circle.x) / len * circle.radius} ${circle.y + (top + 10 - circle.y) / len * circle.radius} L ${left + 540 / 2} ${top + 10}`} />
                    </svg>
                </LineWrapper>
            </div>;

        })
        }

    </div>;
};

export default Locator;
