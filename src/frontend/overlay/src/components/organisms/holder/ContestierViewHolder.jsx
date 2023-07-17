import React, { useEffect, useLayoutEffect } from "react";
import { ContestantViewCorner2 } from "../../molecules/info/ContestantViewCorner2";
import {
    TeamImageWrapper,
    TeamVideoAnimationWrapper,
    TeamVideoWrapper,
    TeamWebRTCGrabberVideoWrapper,
    TeamWebRTCProxyVideoWrapper,
    TeamVideoAnimationWrapperWithFixForOldBrowsers,
} from "./TeamViewHolder";


const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, teamId, isSmall }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ContestantViewCorner2 teamId={teamId} isSmall={isSmall}/>;
    },
    Photo: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </TeamVideoAnimationWrapper>;
    },
    Object: ({ onLoadStatus, url }) => {
        onLoadStatus(true);
        return <TeamVideoAnimationWrapper>
            <object data={url} type="image/svg+xml">
            </object>
        </TeamVideoAnimationWrapper>;
    },
    Video: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, url, audioUrl }) => {
        return <TeamVideoAnimationWrapper>
            {audioUrl && <audio src={audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper url={url} setIsLoaded={onLoadStatus}/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <TeamVideoAnimationWrapperWithFixForOldBrowsers>
            <TeamWebRTCGrabberVideoWrapper {...props}/>
        </TeamVideoAnimationWrapperWithFixForOldBrowsers>;
    },
};

export const ContestierViewHolder = ({ onLoadStatus, media, isSmall }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} {...media}/>;
};
