import React, { useEffect, useLayoutEffect } from "react";
import { ContestantViewCorner } from "../../molecules/info/ContestantViewCorner";
import {
    TeamImageWrapper,
    FullWidthWrapper,
    TeamVideoWrapper,
    TeamWebRTCGrabberVideoWrapper,
    TeamWebRTCProxyVideoWrapper,
    TeamVideoAnimationWrapperWithFixForOldBrowsers,
} from "./TeamViewHolder";


const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, teamId, isSmall }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ContestantViewCorner teamId={teamId} isSmall={isSmall}/>;
    },
    Photo: ({ onLoadStatus, url }) => {
        return <FullWidthWrapper>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </FullWidthWrapper>;
    },
    Object: ({ onLoadStatus, url }) => {
        onLoadStatus(true);
        return <FullWidthWrapper>
            <object data={url} type="image/svg+xml">
            </object>
        </FullWidthWrapper>;
    },
    Video: ({ onLoadStatus, url }) => {
        return <FullWidthWrapper>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted/>
        </FullWidthWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, url, audioUrl }) => {
        return <FullWidthWrapper>
            {audioUrl && <audio src={audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper url={url} setIsLoaded={onLoadStatus}/>
        </FullWidthWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <TeamVideoAnimationWrapperWithFixForOldBrowsers>
            <TeamWebRTCGrabberVideoWrapper {...props}/>
        </TeamVideoAnimationWrapperWithFixForOldBrowsers>;
    },
};

export const ContestantViewHolder = ({ onLoadStatus, media, isSmall }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} {...media}/>;
};
