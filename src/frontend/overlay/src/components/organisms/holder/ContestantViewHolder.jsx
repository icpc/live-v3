import React, { useEffect, useLayoutEffect, useRef } from "react";
import { ContestantViewCorner } from "../../molecules/info/ContestantViewCorner";
import styled from "styled-components";
import { useDispatch } from "react-redux";
import { pushLog } from "../../../redux/debug";
import { GrabberPlayerClient } from "../../../utils/grabber/grabber_player";

export const TeamImageWrapper = styled.img`
  border-radius: ${({ borderRadius }) => borderRadius};
`;

export const TeamVideoWrapper = styled.video`
  position: absolute;
  width: 100%;
  /* height: 100%; */
  bottom: 0;
  aspect-ratio: 16/9;
  object-fit: cover;
  object-position: bottom;
  border-radius: ${({ borderRadius }) => borderRadius};
`;


export const TeamWebRTCProxyVideoWrapper = ({ Wrapper = TeamVideoWrapper, url, setIsLoaded, ...props }) => {
    const dispatch = useDispatch();
    const videoRef = useRef();
    const rtcRef = useRef();
    useEffect(() => {
        setIsLoaded(false);
        rtcRef.current = new RTCPeerConnection();
        rtcRef.current.ontrack = function (event) {
            if (event.track.kind !== "video") {
                return;
            }
            videoRef.current.srcObject = event.streams[0];
            videoRef.current.play();
        };
        rtcRef.current.addTransceiver("video");
        rtcRef.current.addTransceiver("audio");
        rtcRef.current.createOffer()
            .then(offer => {
                rtcRef.current.setLocalDescription(offer);
                return fetch(url, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(offer),
                });
            })
            .then(res => res.json())
            .then(res => rtcRef.current.setRemoteDescription(res))
            .catch(e => console.trace("ERROR featching  webrtc peer connection info: " + e));

        return () => rtcRef.current?.close();
    }, [url]);
    return (<Wrapper
        ref={videoRef}
        onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
        onLoadedData={() => {
            // console.log("Loaded");
            // console.log(videoRef.current.currentTime);
            return setIsLoaded(true);
        }}
        autoPlay
        muted
        {...props}
    />);
};


export const TeamWebRTCGrabberVideoWrapper = ({ Wrapper = TeamVideoWrapper, url, peerName, streamType, credential, onLoadStatus, ...props }) => {
    const dispatch = useDispatch();
    const videoRef = useRef();
    useEffect(() => {
        const client = new GrabberPlayerClient("play", url);
        client.authorize(credential);
        client.on("initialized", () => {
            console.log(`Connecting to grabber peer ${peerName} for stream ${streamType}`);
            client.connect({ peerName: peerName }, streamType, (track) => {
                videoRef.current.srcObject = null;
                videoRef.current.srcObject = track;
                videoRef.current.play();
                console.log(`WebRTCSocket pc2 received remote stream (${peerName}, ${streamType})`);
            });
        });
        client.on("auth:failed", () => {
            dispatch(pushLog(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`));
            console.warn(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`);
        });

        return () => {
            client.close();
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url, peerName, streamType]);

    return (<Wrapper
        ref={videoRef}
        onLoadedData={() => onLoadStatus(true)}
        onError={() => onLoadStatus(false) || dispatch(pushLog("ERROR on loading image in WebRTC widget"))}
        muted
        {...props}/>);
};


export const FullWidthWrapper = styled.div`
  width: 100%;
  border-radius: 16px;
  /* position: absolute; */
  
  /* this is how you make aspect ratio before aspect-ratio. 
     Do not remove until the whole world starts using modern VMix
     Sadly this hack will cut off the bottom of the picture
     But since all we show here is 16/9 images - it's ok.
     Have to deal with it.
  */
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  box-sizing: border-box;
`;

const ContestantViewHolderCorner = styled(ContestantViewCorner)`
  z-index: 1; /* Fixme when there is a proper grid in TeamView */
  grid-column-end: 3;
  grid-row-start: 1;
  grid-row-end: ${props => props.hasPInP ? 2 : 3};
  justify-self: end;
  align-self: end;
`;

const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, hasPInP, ...props }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ContestantViewHolderCorner hasPInP={hasPInP} {...props}/>;
    },
    Photo: ({ onLoadStatus, url, className }) => {
        return <FullWidthWrapper className={className}>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </FullWidthWrapper>;
    },
    Object: ({ onLoadStatus, url, className }) => {
        onLoadStatus(true);
        return <FullWidthWrapper className={className}>
            <object data={url} type="image/svg+xml">
            </object>
        </FullWidthWrapper>;
    },
    Video: ({ onLoadStatus, url, className }) => {
        return <FullWidthWrapper className={className}>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted/>
        </FullWidthWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, url, audioUrl, className }) => {
        return <FullWidthWrapper className={className}>
            {audioUrl && <audio src={audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper url={url} setIsLoaded={onLoadStatus}/>
        </FullWidthWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <FullWidthWrapper>
            <TeamWebRTCGrabberVideoWrapper {...props}/>
        </FullWidthWrapper>;
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

export const Achievement = ({ src, onLoadStatus, className }) => {
    return <AchievementWrapper className={className}>
        <TeamImageWrapper src={src} onLoad={() => onLoadStatus(true)}/>
    </AchievementWrapper>;
};

export const ContestantViewHolder = ({ onLoadStatus, media, isSmall, hasPInP, className }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return null;
    }
    if (!media.isMedia && media.type === "Photo") {
        return <Achievement src={media.url} onLoadStatus={onLoadStatus} className={className}/>;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} hasPInP={hasPInP} {...media} className={className}/>;
};
