import { GrabberSocket } from "./sockets";

export class GrabberPlayerClient {
    constructor(mode, url) {
        this.pc = null;
        this.peerConnectionConfig = null;
        this.peersStatus = null;
        this.participantsStatus = null;
        this.target = new EventTarget();
        this.ws = new GrabberSocket(
            (url ?? "") + "/ws/player/" + (mode === "play" ? "play" : "admin"),
        );
        this._setupWS();
    }

    _setupWS() {
        this.ws.on("auth:request", () => {
            this.target.dispatchEvent(new CustomEvent("auth:request", {}));
        });

        this.ws.on("auth:failed", () => {
            this.ws.close();
            this.target.dispatchEvent(new CustomEvent("auth:failed", {}));
        });

        this.ws.on("init_peer", ({ initPeer: { pcConfig } }) => {
            this.peerConnectionConfig = pcConfig;
            console.debug("WebRTCGrabber: connection initialized");
            this.target.dispatchEvent(new CustomEvent("initialized", {}));
        });

        this.ws.on("peers", ({ peersStatus, participantsStatus }) => {
            this.peersStatus = peersStatus ?? [];
            this.participantsStatus = participantsStatus ?? [];
            this.target.dispatchEvent(
                new CustomEvent("peers", {
                    detail: [peersStatus, participantsStatus],
                }),
            );
        });

        this.ws.on(
            "offer_answer",
            async ({ offerAnswer: { peerId, answer } }) => {
                console.debug(`WebRTCGrabber: got offer_answer from ${peerId}`);
                await this.pc?.setRemoteDescription(answer);
            },
        );

        this.ws.on("offer:failed", () => {
            this.ws.close();
            this.target.dispatchEvent(new CustomEvent("connection:failed", {}));
        });

        this.ws.on("grabber_ice", async ({ ice: { peerId, candidate } }) => {
            console.debug(`WebRTCGrabber: got grabber_ice from ${peerId}`);
            await this.pc?.addIceCandidate(candidate);
        });
    }

    formatPeerInfo(peerInfo) {
        return peerInfo.peerId ?? `{${peerInfo.peerName}}`;
    }

    authorize(credential) {
        this.ws.emit("auth", { playerAuth: { credential: credential } });
    }

    connect(peerInfo, streamType, onVideoTrack) {
        const pc = new RTCPeerConnection(this.peerConnectionConfig);
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
            });
        });

        this._closePeerConnection();
        this.pc = pc;

        pc.createOffer().then((offer) => {
            pc.setLocalDescription(offer);
            this.ws.emit("offer", {
                offer: { ...peerInfo, offer, streamType },
            });
            console.debug(
                `WebRTCGrabber: sending offer to ${this.formatPeerInfo(peerInfo)} ${streamType} ...`,
            );
        });
    }

    on(eventName, callback) {
        this.target.addEventListener(eventName, (e) => callback(e.detail));
    }

    _closePeerConnection() {
        this.pc?.close();
        this.pc = null;
    }

    close() {
        this.ws?.close();
        this._closePeerConnection();
    }
}
