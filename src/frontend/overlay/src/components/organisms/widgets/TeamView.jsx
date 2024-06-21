import React, { useState, useEffect, useMemo } from "react";
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
  display: block;
  position: relative;
`;

const TeamViewLoaderContainer = styled.div`
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

function TeamViewContent({ content, mediaContent, setLoadedComponents, location, isSmall }) {
    const hasPInP = content.filter(e => !e.isMedia).concat(mediaContent).filter((c) => c.pInP).length > 0;

    return <TeamViewWrapper sizeX={location.sizeX} sizeY={location.sizeY}>
        {content.filter(e => !e.isMedia).concat(mediaContent).map((c, index) => {
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

const TeamViewLoader = ({ settings, content, location, transitionState, isLoaded, setLoadedComponents }) => {
    const mediaContent = content.filter(e => e.isMedia).map((e, index) => ({ ...e, pInP: index > 0 }));
    const isSmall = settings.position !== "SINGLE_TOP_RIGHT";
    const passedProps = {
        mediaContent,
        content,
        settings,
        setLoadedComponents,
        location
    };

    return (
        <TeamViewLoaderContainer
            show={isLoaded}
            animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
            animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
        >
            {settings.position === "PVP_TOP" || settings.position === "PVP_BOTTOM" ?
                <PVP {...passedProps}/> :
                <TeamViewContent isSmall={isSmall} {...passedProps}/>
            }
        </TeamViewLoaderContainer>

    );
};

const useStableObj = (value) => {
    const [stableContent, setStableContent] = useState(value);
    useEffect(() => {
        if (JSON.stringify(value) !== JSON.stringify(stableContent)) {
            setStableContent(value);
        }
    }, [value]);
    return stableContent;
};

export const TeamView = ({ widgetData: { settings, location }, transitionState }) => {
    const stableContent = useStableObj(settings.content);

    const [content1, setContent1] = useState(null);
    const [content2, setContent2] = useState(null);

    const [counter, setCounter] = useState(1);
    const [loaded1, setLoaded1] = useState(0);
    const [loaded2, setLoaded2] = useState(0);

    const isLoaded1 = (content1 && loaded1 === (1 << content1.length) - 1) ?? false;
    const isLoaded2 = (content2 && loaded2 === (1 << content2.length) - 1) ?? false;

    useEffect(() => {
        console.info("TeamView: switch content to instance", 3 - counter);
        if (counter === 0) { // initialized
            setCounter(2);
        } else if (Math.abs(counter) === 2) { // now let switch from 2 to 1
            setLoaded1(0);
            setContent1(stableContent);
            setCounter(-1);
        } else { // now let switch from 2 to 1
            setContent2(stableContent);
            setLoaded2(0);
            setCounter(-2);
        }
        // setContent1(stableContent);
        // setCounter(c => c + 1);
    }, [stableContent]);
    useEffect(() => {
        if (counter === -1 && isLoaded1) {
            console.info("TeamView: instance 1 loaded, switch");
            setContent2(null);
            setLoaded2(0);
            setCounter(1);
        }
    }, [counter, setCounter, isLoaded1]);
    useEffect(() => {
        if (counter === -2 && isLoaded2) {
            console.info("TeamView: instance 2 loaded, switch");
            setContent1(null);
            setLoaded1(0);
            setCounter(2);
        }
    }, [counter, setCounter, isLoaded2]);


    // console.info("TeamView render",settings.content, isLoaded1, isLoaded2, content1 !== null, content2 !== null);

    return (
        <TeamViewContainer>
            {content1 && (
                <TeamViewLoader
                    settings={settings}
                    content={content1}
                    location={location}
                    transitionState={transitionState}
                    isLoaded={isLoaded1 && (counter !== 2 || !isLoaded2)}
                    setLoadedComponents={setLoaded1}
                />
            )}
            {content2 && (
                <TeamViewLoader
                    settings={settings}
                    content={content2}
                    location={location}
                    transitionState={transitionState}
                    isLoaded={isLoaded2 && (counter !== 1 || !isLoaded1)}
                    setLoadedComponents={setLoaded2}
                />
            )}
        </TeamViewContainer>
    );
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = c.TEAM_VIEW_APPEAR_TIME;
