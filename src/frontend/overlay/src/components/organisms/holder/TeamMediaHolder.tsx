import styled from "styled-components";
import { MediaType } from "@shared/api";
import { useQueryParams } from "@/utils/query-params";
import React, { useEffect, useRef } from "react";
import Hls from "hls.js";
import mpegts from "mpegts.js";
import { GrabberPlayerClient } from "@/utils/grabber/grabber_player";

type MediaHolderProps<M extends MediaType> = {
    media: M;
    onLoadStatus: (currentStatus: boolean) => void;
    className?: string;
};

const useIsTeamMediaAudio = () => {
    const queryParams = useQueryParams();
    return (
        (!queryParams.has("teamMediaAudio") && window["obsstudio"]) ||
        (queryParams.has("teamMediaAudio") &&
            queryParams.get("teamMediaAudio") !== "false")
    );
};

const MediaWrapper = styled.div<{ $vertical?: boolean }>`
    overflow: hidden;
    text-align: right;

    width: 100%;
    height: ${(props) => (props.$vertical ? "100%" : "auto")};
    max-width: 100%;
    max-height: 100%;
`;

const ImgContainer = styled.img<{ $vertical?: boolean }>`
    width: ${(props) => (props.$vertical ? "auto" : "100%")};
    height: ${(props) => (props.$vertical ? "100%" : "auto")};
    overflow: hidden;
`;

const VideoContainer = styled.video<{ $vertical?: boolean }>`
    width: ${(props) => (props.$vertical ? "auto" : "100%")};
    height: ${(props) => (props.$vertical ? "100%" : "auto")};
    overflow: hidden;
`;

export const ImageMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, vertical },
}: MediaHolderProps<MediaType.Image>) => {
    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <ImgContainer
                src={url}
                onLoad={() => onLoadStatus(true)}
                $vertical={vertical}
            />
        </MediaWrapper>
    );
};

export const VideoMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, vertical },
}: MediaHolderProps<MediaType.Video>) => {
    const audio = useIsTeamMediaAudio();
    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <VideoContainer
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted={!audio}
                controls={false}
                $vertical={vertical}
            />
        </MediaWrapper>
    );
};

export const HLSVideoMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, jwtToken, vertical },
}: MediaHolderProps<MediaType.HLSVideo>) => {
    const audio = useIsTeamMediaAudio();

    const playerRef = useRef<HTMLVideoElement>(null);
    useEffect(() => {
        let hls: Hls;

        function _initPlayer() {
            if (hls != null) {
                hls.destroy();
            }

            const newHls = new Hls({
                enableWorker: false,
                xhrSetup: (xhr) => {
                    if (jwtToken) {
                        xhr.setRequestHeader(
                            "Authorization",
                            "Bearer " + jwtToken,
                        );
                    }
                },
            });

            if (playerRef.current != null) {
                newHls.attachMedia(playerRef.current);
            }

            newHls.on(Hls.Events.MEDIA_ATTACHED, () => {
                newHls.loadSource(url);

                newHls.on(Hls.Events.MANIFEST_PARSED, () => {
                    // autoplay
                    playerRef?.current
                        ?.play()
                        .catch(() =>
                            console.warn(
                                "Unable to autoplay hls video prior to user interaction with the dom",
                            ),
                        );
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
    }, [url, jwtToken]);

    // If Media Source is supported, use HLS.js to play video
    // And fallback to using a regular video player if HLS is supported by default in the user's browser
    const src = Hls.isSupported() ? undefined : url;

    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <VideoContainer
                ref={playerRef}
                src={src}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted={!audio}
                controls={false}
                $vertical={vertical}
            />
        </MediaWrapper>
    );
};

export const M2tsVideoMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, vertical },
}: MediaHolderProps<MediaType.M2tsVideo>) => {
    const audio = useIsTeamMediaAudio();

    const videoRef = useRef<HTMLVideoElement>(null);
    useEffect(() => {
        onLoadStatus(false);
        if (videoRef.current) {
            const player = mpegts.createPlayer({
                type: "mpegts",
                isLive: true,
                url: url,
            });
            player.attachMediaElement(videoRef.current);
            player.load();
            return () => {
                player.destroy();
            };
        }
        return () => {
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url]);

    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <VideoContainer
                ref={videoRef}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted={!audio}
                controls={false}
                $vertical={vertical}
            />
        </MediaWrapper>
    );
};

export const WebRTCGrabberMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, peerName, streamType, credential, vertical },
}: MediaHolderProps<MediaType.WebRTCGrabberConnection>) => {
    const audio = useIsTeamMediaAudio();

    const videoRef = useRef<HTMLVideoElement>(null);
    useEffect(() => {
        const client = new GrabberPlayerClient("play", url);
        client.authorize(credential);
        client.on("initialized", () => {
            console.info(
                `WebRTCGrabber: Connecting to peer ${peerName} for stream ${streamType}`,
            );
            client.connect({ peerName: peerName }, streamType, (track) => {
                videoRef.current.srcObject = null;
                videoRef.current.srcObject = track;
                videoRef.current.play();
                console.info(
                    `WebRTCGrabber: pc2 received remote stream (${peerName}, ${streamType})`,
                );
            });
        });
        client.on("auth:failed", () => {
            console.warn(
                `WebRTCGrabber: Connection failed from ${url} peerName ${peerName}. Incorrect credential`,
            );
        });
        client.on("connection:failed", () => {
            console.warn(
                `WebRTCGrabber: Connection failed from ${url} peerName ${peerName}. No such peer with ${streamType}`,
            );
            onLoadStatus(true);
        });

        return () => {
            client.close();
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url, peerName, streamType]);

    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <VideoContainer
                ref={videoRef}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                muted={!audio}
                controls={false}
                $vertical={vertical}
            />
        </MediaWrapper>
    );
};

export const WebRTCProxyMediaHolder = ({
    onLoadStatus,
    className,
    media: { url, audioUrl, vertical },
}: MediaHolderProps<MediaType.WebRTCProxyConnection>) => {
    const audio = useIsTeamMediaAudio();

    const videoRef = useRef<HTMLVideoElement>(null);
    const rtcRef = useRef<RTCPeerConnection>(null);
    useEffect(() => {
        onLoadStatus(false);
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
        rtcRef.current
            .createOffer()
            .then((offer) => {
                rtcRef.current.setLocalDescription(offer);
                return fetch(url, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(offer),
                });
            })
            .then((res) => res.json())
            .then((res) => rtcRef.current.setRemoteDescription(res))
            .catch((e) =>
                console.trace(
                    "ERROR featching  webrtc peer connection info: " + e,
                ),
            );

        return () => rtcRef.current?.close();
    }, [url]);

    return (
        <MediaWrapper $vertical={vertical} className={className}>
            <VideoContainer
                ref={videoRef}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                muted={!audio}
                controls={false}
                $vertical={vertical}
            />
            {audioUrl && (
                <audio
                    src={audioUrl}
                    onLoadedData={() => onLoadStatus(true)}
                    autoPlay
                />
            )}
        </MediaWrapper>
    );
};

export const ObjectMediaHolder = ({
    onLoadStatus,
    media: { url },
}: MediaHolderProps<MediaType.Object>) => {
    useEffect(() => {
        onLoadStatus(true);
    }, [onLoadStatus]);
    return <object data={url} type="image/svg+xml"></object>;
};

export const TeamMediaHolder = ({
    media,
    ...props
}: MediaHolderProps<MediaType>) => {
    switch (media.type) {
        case MediaType.Type.Image:
            return (
                <ImageMediaHolder media={media} {...props}></ImageMediaHolder>
            );
        case MediaType.Type.Object:
            return (
                <ObjectMediaHolder media={media} {...props}></ObjectMediaHolder>
            );
        case MediaType.Type.Video:
            return (
                <VideoMediaHolder media={media} {...props}></VideoMediaHolder>
            );
        case MediaType.Type.HLSVideo:
            return (
                <HLSVideoMediaHolder
                    media={media}
                    {...props}
                ></HLSVideoMediaHolder>
            );
        case MediaType.Type.M2tsVideo:
            return (
                <M2tsVideoMediaHolder
                    media={media}
                    {...props}
                ></M2tsVideoMediaHolder>
            );
        case MediaType.Type.WebRTCGrabberConnection:
            return (
                <WebRTCGrabberMediaHolder
                    media={media}
                    {...props}
                ></WebRTCGrabberMediaHolder>
            );
        case MediaType.Type.WebRTCProxyConnection:
            return (
                <WebRTCProxyMediaHolder
                    media={media}
                    {...props}
                ></WebRTCProxyMediaHolder>
            );
        default:
            return null;
    }
};
