import React, { useEffect, useLayoutEffect } from "react";
import { ContestantViewCorner } from "../../molecules/info/ContestantViewCorner";
import {
    TeamImageWrapper,
    FullWidthWrapper,
    TeamVideoWrapper,
    TeamWebRTCGrabberVideoWrapper,
    TeamWebRTCProxyVideoWrapper,
} from "./TeamViewHolder";
import styled from "styled-components";


const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, teamId, isSmall, hasPInP }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        console.log(teamId);
        return <ContestantViewCorner teamId={teamId} isSmall={isSmall} hasPInP={hasPInP}/>;
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

export const AchievementWrapper = styled.div`
  width: 100%;
  position: absolute;
  z-index: -1;
  top: 0;
  border-radius: 16px;
  height: 100%;
`;

export const Achievement = ({src, onLoadStatus}) => {
    console.log(src)
    return <AchievementWrapper>
        <TeamImageWrapper src={src} onLoad={() => onLoadStatus(true)}/>
    </AchievementWrapper>;
};

export const ContestantViewHolder = ({ onLoadStatus, media, isSmall, hasPInP }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    if (!media.isMedia && media.type === "Photo") {
        return <Achievement src={media.url} onLoadStatus={onLoadStatus}/>
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} hasPInP={hasPInP} {...media}/>;
};
