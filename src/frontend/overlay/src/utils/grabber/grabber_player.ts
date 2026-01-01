import { GrabberSocket } from "./sockets";

interface PeerInfo {
    peerId?: string;
    peerName?: string;
}

interface AuthPayload extends Record<string, unknown> {
    playerAuth: {
        credential: string;
    };
}

interface OfferPayload extends Record<string, unknown> {
    offer: PeerInfo & {
        offer: RTCSessionDescriptionInit;
        streamType: string;
    };
}

interface IcePayload extends Record<string, unknown> {
    ice: PeerInfo & {
        candidate: RTCIceCandidate | null;
    };
}

type OnVideoTrackCallback = (
    stream: MediaStream,
    peerInfo: PeerInfo,
    streamType: string,
) => void;

export class GrabberPlayerClient {
    private pc: RTCPeerConnection | null = null;
    private peerConnectionConfig: RTCConfiguration | null = null;
    private peersStatus: unknown[] | null = null;
    private participantsStatus: unknown[] | null = null;
    private target: EventTarget;
    private ws: GrabberSocket;

    constructor(mode: string, url?: string) {
        this.target = new EventTarget();
        this.ws = new GrabberSocket(
            (url ?? "") + "/ws/player/" + (mode === "play" ? "play" : "admin"),
        );
        this._setupWS();
    }

    private _setupWS() {
        this.ws.on("auth:request", () => {
            this.target.dispatchEvent(new CustomEvent("auth:request", {}));
        });

        this.ws.on("auth:failed", () => {
            this.ws.close();
            this.target.dispatchEvent(new CustomEvent("auth:failed", {}));
        });

        this.ws.on("init_peer", (detail: unknown) => {
            const { initPeer } = detail as {
                initPeer: { pcConfig: RTCConfiguration };
            };
            this.peerConnectionConfig = initPeer.pcConfig;
            console.debug("WebRTCGrabber: connection initialized");
            this.target.dispatchEvent(new CustomEvent("initialized", {}));
        });

        this.ws.on("peers", (detail: unknown) => {
            const { peersStatus, participantsStatus } = detail as {
                peersStatus?: unknown[];
                participantsStatus?: unknown[];
            };
            this.peersStatus = peersStatus ?? [];
            this.participantsStatus = participantsStatus ?? [];
            this.target.dispatchEvent(
                new CustomEvent("peers", {
                    detail: [peersStatus, participantsStatus],
                }),
            );
        });

        this.ws.on("offer_answer", async (detail: unknown) => {
            const { offerAnswer } = detail as {
                offerAnswer: {
                    peerId: string;
                    answer: RTCSessionDescriptionInit;
                };
            };
            console.debug(
                `WebRTCGrabber: got offer_answer from ${offerAnswer.peerId}`,
            );
            await this.pc?.setRemoteDescription(offerAnswer.answer);
        });

        this.ws.on("offer:failed", () => {
            this.ws.close();
            this.target.dispatchEvent(new CustomEvent("connection:failed", {}));
        });

        this.ws.on("grabber_ice", async (detail: unknown) => {
            const { ice } = detail as {
                ice: { peerId: string; candidate: RTCIceCandidateInit };
            };
            console.debug(`WebRTCGrabber: got grabber_ice from ${ice.peerId}`);
            await this.pc?.addIceCandidate(ice.candidate);
        });
    }

    private formatPeerInfo(peerInfo: PeerInfo): string {
        return peerInfo.peerId ?? `{${peerInfo.peerName}}`;
    }

    authorize(credential: string) {
        this.ws.emit("auth", {
            playerAuth: { credential: credential },
        } as AuthPayload);
    }

    connect(
        peerInfo: PeerInfo,
        streamType: string,
        onVideoTrack: OnVideoTrackCallback,
    ) {
        const pc = new RTCPeerConnection(
            this.peerConnectionConfig ?? undefined,
        );
        pc.addTransceiver("video");
        pc.addTransceiver("audio");

        pc.addEventListener("track", (e) => {
            console.debug("WebRTCGrabber: got track");
            if (e.track.kind === "video" && e.streams.length > 0) {
                onVideoTrack(e.streams[0], peerInfo, streamType);
            }
        });

        pc.addEventListener("icecandidate", (event) => {
            console.debug(
                `WebRTCGrabber: sending ice to ${this.formatPeerInfo(peerInfo)}`,
            );
            this.ws.emit("player_ice", {
                ice: { ...peerInfo, candidate: event.candidate },
            } as IcePayload);
        });

        this._closePeerConnection();
        this.pc = pc;

        pc.createOffer().then((offer) => {
            pc.setLocalDescription(offer);
            this.ws.emit("offer", {
                offer: { ...peerInfo, offer, streamType },
            } as OfferPayload);
            console.debug(
                `WebRTCGrabber: sending offer to ${this.formatPeerInfo(peerInfo)} ${streamType} ...`,
            );
        });
    }

    on(eventName: string, callback: (detail?: unknown) => void) {
        this.target.addEventListener(eventName, (e) =>
            callback((e as CustomEvent).detail),
        );
    }

    private _closePeerConnection() {
        this.pc?.close();
        this.pc = null;
    }

    close() {
        this.ws?.close();
        this._closePeerConnection();
    }
}
