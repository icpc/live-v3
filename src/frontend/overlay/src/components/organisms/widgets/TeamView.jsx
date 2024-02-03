import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import c from "../../../config";
import { ContestantViewHolder } from "../holder/ContestantViewHolder";
import PVP from "./PVP";

const slideIn = keyframes`
  from {
    opacity: 0;
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
    opacity: 0;
  }
`;

const TeamViewContainer = styled.div`
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  flex-direction: column;
  justify-content: start;
  align-items: flex-end;
  position: relative;
  animation: ${props => props.animation} ${c.TEAM_VIEW_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;

const TeamViewPInPWrapper = styled.div`
  width: 100%;
  height: 100%;
  grid-column-start: 2;
  grid-column-end: 3;
  grid-row-start: 2;
  grid-row-end: 4;
  position: relative;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const TeamViewWrapper = styled.div`
  width: 100%;
  height: 100%;
  display: grid;
  justify-content: end;
  grid-template-columns: ${({ sizeX, sizeY }) => `${sizeX - 2 * (Math.max(sizeY - sizeX * 9 / 16, 100)) * 16 / 9}px`} 
                          ${({ sizeX, sizeY }) => `${2 * (Math.max(sizeY - sizeX * 9 / 16, 100)) * 16 / 9}px`};
  grid-template-rows: ${({ sizeX, sizeY }) => `${sizeY - 2 * Math.max(sizeY - sizeX * 9 / 16, 100)}px`} 
                      ${({ sizeX, sizeY }) => `${Math.max(sizeY - sizeX * 9 / 16, 100)}px`} 
                      ${({ sizeX, sizeY }) => `${Math.max(sizeY - sizeX * 9 / 16, 100)}px`};
`;


const TeamViewContestantViewHolder = styled(ContestantViewHolder)`
    top: 0; /* # FIXME: fuck this. */
`;

function TeamViewContent({ mediaContent, settings, setLoadedComponents, location, isSmall }) {
    const hasPInP = settings.content.filter(e => !e.isMedia).concat(mediaContent).filter((c) => c.pInP).length > 0;

    return <TeamViewWrapper sizeX={location.sizeX} sizeY={location.sizeY}>
        {settings.content.filter(e => !e.isMedia).concat(mediaContent).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            const component = <TeamViewContestantViewHolder key={c.type + index} onLoadStatus={onLoadStatus} media={c}
                isSmall={isSmall} hasPInP={hasPInP}/>;
            if (c.pInP) {
                return <TeamViewPInPWrapper key={c.type + index} sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
            }
            return component;
        })}
    </TeamViewWrapper>;
}

export const TeamView = ({ widgetData: { settings, location }, transitionState }) => {
    const [loadedComponents, setLoadedComponents] = useState(0);
    const isLoaded = loadedComponents === (1 << settings.content.length) - 1;
    const mediaContent = settings.content.filter(e => e.isMedia).map((e, index) => ({ ...e, pInP: index > 0 }));
    const isSmall = settings.position !== "SINGLE_TOP_RIGHT";
    const passedProps = {
        mediaContent,
        settings,
        setLoadedComponents,
        location
    };
    return <TeamViewContainer
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        {settings.position === "PVP_TOP" || settings.position === "PVP_BOTTOM" ?
            <PVP {...passedProps}/> :
            <TeamViewContent isSmall={isSmall} {...passedProps}/>
        }
    </TeamViewContainer>;
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = c.TEAM_VIEW_APPEAR_TIME;
