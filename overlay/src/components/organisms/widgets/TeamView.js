import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import { TEAM_VIEW_APPEAR_TIME } from "../../../config";
import { TeamViewHolder } from "../holder/TeamVeiwHolder";
import PVP from "./PVP";

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

const TeamViewContainer = styled.div`
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  flex-direction: column;
  justify-content: start;
  align-items: flex-end;
  position: relative;
  animation: ${props => props.animation} ${TEAM_VIEW_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;

const TeamViewPInPWrapper = styled.div`
  position: absolute;
  width: ${({ sizeX }) => `${sizeX * 0.4}px`};
  height: ${({ sizeX }) => `${sizeX * 0.5625 * 0.4}px`};
  right: 0;
  top: ${({ sizeX }) => `${sizeX * 0.5625 * 0.6}px`};
`;


function TeamViewWrapper({ mediaContent, settings, setLoadedComponents, location }) {
    return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
        const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
        const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
        if (c.pInP) {
            return <TeamViewPInPWrapper sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
        }
        return component;
    });
}

export const TeamView = ({ widgetData: { settings, location }, transitionState }) => {
    const [loadedComponents, setLoadedComponents] = useState(0);
    const isLoaded = loadedComponents === (1 << settings.content.length) - 1;
    const mediaContent = settings.content.filter(e => e.isMedia).map((e, index) => ({ ...e, pInP: index > 0 }));
    return <TeamViewContainer
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        {settings.position === "PVP_TOP" || settings.position === "PVP_BOTTOM" ?
            <PVP mediaContent={mediaContent} settings={settings} setLoadedComponents={setLoadedComponents} location={location}/> :
            <TeamViewWrapper setLoadedComponents={setLoadedComponents} mediaContent={mediaContent} settings={settings} location={location}/>
        }
    </TeamViewContainer>;
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = TEAM_VIEW_APPEAR_TIME;

export default TeamView;
