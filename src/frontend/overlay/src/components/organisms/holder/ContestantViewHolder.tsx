import React, { FC, useEffect, useLayoutEffect, useRef } from "react";
import { ContestantViewCorner } from "../../molecules/info/ContestantViewCorner";
import styled from "styled-components";
import { pushLog } from "@/redux/debug";
import { GrabberPlayerClient } from "../../../utils/grabber/grabber_player";
import { useQueryParams } from "@/utils/query-params";
import { useAppDispatch } from "@/redux/hooks";
import { MediaType } from "@shared/api";
import c from "../../../config";
import mpegts from "mpegts.js";
import Hls from "hls.js";

// export const TeamImageWrapper = styled.img /*`
//   // border-radius: ${({ borderRadius }) => borderRadius};
// `*/;

// https://usefulangle.com/post/142/css-video-aspect-ratio
// export const TeamVideoWrapper = styled.video/*`
//   position: absolute;
//   width: 100%;
//   // height: 100%;
//   bottom: 0;
//   aspect-ratio: 16/9;
//   object-fit: cover;
//   object-position: bottom;
//   border-radius: ${({ borderRadius }) => borderRadius};
// `*/;

const VideoWrapper = styled.video`
  position: absolute;
  width: 100%;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const ImgWrapper = styled.img`
  position: absolute;
  width: 100%;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

export const TeamM2tsVideoWrapper = ({ url, setIsLoaded }) => {
    const dispatch = useAppDispatch();
    const videoRef = useRef<HTMLVideoElement>();
    useEffect(() => {
        setIsLoaded(false);
        if (videoRef.current) {
            const player = mpegts.createPlayer({
                type: "mpegts",
                isLive: true,
                url: url,
                // muted: !queryParams.has("teamview_audio"),
            });
            player.attachMediaElement(videoRef.current);
            player.load();
            return () => {
                player.destroy();
            };
        }
        return ()  => {
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url]);
    const queryParams = useQueryParams();
    return (<VideoWrapper
        ref={videoRef}
        onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
        onLoadedData={() =>
            // console.log("Loaded");
            // console.log(videoRef.current.currentTime);
            setIsLoaded(true)
        }
        autoPlay
        muted={!queryParams.has("teamview_audio")}/>);
};


export const TeamWebRTCProxyVideoWrapper = ({ url, setIsLoaded, ...props }) => {
    const dispatch = useAppDispatch();
    const videoRef = useRef<HTMLVideoElement>();
    const rtcRef = useRef<RTCPeerConnection>();
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
    const queryParams = useQueryParams();
    return (<VideoWrapper
        ref={videoRef}
        onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
        onLoadedData={() => {
            // console.log("Loaded");
            // console.log(videoRef.current.currentTime);
            return setIsLoaded(true);
        }}
        autoPlay
        muted={!queryParams.has("teamview_audio")}
        {...props}
    />);
};


export const TeamWebRTCGrabberVideoWrapper = ({ media: { url, peerName, streamType, credential }, onLoadStatus, ...props }) => {
    const dispatch = useAppDispatch();
    const videoRef = useRef<HTMLVideoElement>();
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
        client.on("connection:failed", () => {
            dispatch(pushLog(`Webrtc content failed from ${url} peerName ${peerName}. No such peer with ${streamType}`));
            console.warn(`Webrtc content failed from ${url} peerName ${peerName}. No such peer with ${streamType}`);
            onLoadStatus(true);
        });

        return () => {
            client.close();
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url, peerName, streamType]);

    const queryParams = useQueryParams();

    return (<VideoWrapper
        ref={videoRef}
        onLoadedData={() => onLoadStatus(true)}
        onError={() => onLoadStatus(false) || dispatch(pushLog("ERROR on loading image in WebRTC widget"))}
        muted={!queryParams.has("teamview_audio")}
        {...props}/>);
};

type HlsPlayerProps = {
    src: string;
    jwtToken?: string;
    // onCanPlay: () => void;
    onError?: () => void;
} & React.VideoHTMLAttributes<HTMLVideoElement>

function HlsPlayer({
    src,
    autoPlay,
    jwtToken,
    // onCanPlay,
    ...props
}: HlsPlayerProps) {
    const playerRef = useRef<HTMLVideoElement>();
    useEffect(() => {
        // onCanPlay();
        let hls: Hls;

        function _initPlayer() {
            if (hls != null) {
                hls.destroy();
            }

            const newHls = new Hls({
                enableWorker: false,
                xhrSetup: (xhr) => {
                    if (jwtToken) {
                        xhr.setRequestHeader("Authorization", "Bearer " + jwtToken);
                    }
                },
            });

            if (playerRef.current != null) {
                newHls.attachMedia(playerRef.current);
            }

            newHls.on(Hls.Events.MEDIA_ATTACHED, () => {
                newHls.loadSource(src);

                newHls.on(Hls.Events.MANIFEST_PARSED, () => {
                    if (autoPlay) {
                        playerRef?.current
                            ?.play()
                            .catch(() =>
                                console.log(
                                    "Unable to autoplay prior to user interaction with the dom."
                                )
                            );
                    }
                });
            });

            newHls.on(Hls.Events.ERROR, function (event, data) {
                if (data.fatal) {
                    switch (data.type) {
                    case Hls.ErrorTypes.NETWORK_ERROR:
                        newHls.startLoad();
                        break;
                    case Hls.ErrorTypes.MEDIA_ERROR:
                        newHls.recoverMediaError();
                        break;
                    default:
                        _initPlayer();
                        break;
                    }
                }
            });

            hls = newHls;
        }

        // Check for Media Source support
        if (Hls.isSupported()) {
            _initPlayer();
        }

        return () => {
            if (hls != null) {
                hls.destroy();
            }
        };
    }, [autoPlay, src, jwtToken]);

    // If Media Source is supported, use HLS.js to play video
    if (Hls.isSupported()) return <VideoWrapper ref={playerRef} autoPlay={autoPlay} controls={false} {...props} />;

    // Fallback to using a regular video player if HLS is supported by default in the user's browser
    return <VideoWrapper ref={playerRef} src={src} autoPlay={autoPlay} controls={false} {...props} />;
}

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

const ContestantViewHolderCorner = styled(ContestantViewCorner)<{hasPInP: boolean}>`
  z-index: 1; /* Fixme when there is a proper grid in TeamView */
  grid-column-end: 3;
  grid-row-start: 1;
  grid-row-end: ${props => props.hasPInP ? 2 : 3};
  justify-self: end;
  align-self: end;
`;

interface ComponentRenderProps<M extends MediaType> { // not sure if extends is the right keyword here
    onLoadStatus: (status: boolean) => void,
    hasPInP?: boolean,
    isSmall: boolean,
    className?: string,
    media: M
}
const teamViewComponentRender: {
    [key in MediaType.Type]: FC<ComponentRenderProps<Extract<MediaType, { type: key }>>> // typescript magic
} = {
    TaskStatus: ({ onLoadStatus, hasPInP, isSmall, className, media }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ContestantViewHolderCorner hasPInP={hasPInP} isSmall={isSmall} className={className} {...media}/>;
    },
    Image: ({ onLoadStatus, className, media }) => {
        return <FullWidthWrapper className={className}>
            <ImgWrapper src={media.url} onLoad={() => onLoadStatus(true)}/>
        </FullWidthWrapper>;
    },
    Object: ({ onLoadStatus, className, media }) => {
        onLoadStatus(true);
        return <FullWidthWrapper className={className}>
            <object data={media.url} type="image/svg+xml">
            </object>
        </FullWidthWrapper>;
    },
    Video: ({ onLoadStatus, className, media }) => {
        const queryParams = useQueryParams();
        return <FullWidthWrapper className={className}>
            <VideoWrapper
                src={media.url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted={!queryParams.has("teamview_audio")}
            />
        </FullWidthWrapper>;
    },
    HLSVideo: ({ onLoadStatus, media, ...props }) => {
        const queryParams = useQueryParams();
        return <FullWidthWrapper>
            <HlsPlayer
                src={media.url}
                jwtToken={media.jwtToken}
                autoPlay
                loop
                muted={!queryParams.has("teamview_audio")}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                {...props}
            />
        </FullWidthWrapper>;
    },
    M2tsVideo: ({ onLoadStatus, className, media }) => {
        return <FullWidthWrapper className={className}>
            <TeamM2tsVideoWrapper url={media.url} setIsLoaded={onLoadStatus}/>
        </FullWidthWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, className, media }) => {
        return <FullWidthWrapper className={className}>
            {media.audioUrl && <audio src={media.audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper url={media.url} setIsLoaded={onLoadStatus}/>
        </FullWidthWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <FullWidthWrapper>
            <TeamWebRTCGrabberVideoWrapper {...props}/>
        </FullWidthWrapper>;
    }
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
        <img src={src} onLoad={() => onLoadStatus(true)}/>
    </AchievementWrapper>;
};

interface ContestantViewHolderProps {
    onLoadStatus: (status: boolean) => void,
    media: MediaType,
    isSmall?: boolean,
    hasPInP?: boolean,
    className?: string
}
export const ContestantViewHolder = ({ onLoadStatus, media, isSmall, hasPInP, className }: ContestantViewHolderProps) => {
    const Component = teamViewComponentRender[media.type] as FC<ComponentRenderProps<MediaType>>; // some more typescript magic
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            []);
        return null;
    }
    if (!media.isMedia && media.type === "Image") { // TODO: why only Image?
        return <Achievement src={media.url} onLoadStatus={onLoadStatus} className={className}/>;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} hasPInP={hasPInP} media={media} className={className}/>;
};
