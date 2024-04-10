import { GrabberSocket } from "./sockets";

export class GrabberPlayerClient {
    constructor(mode, url) {
        this.pc = null;
        this.peerConnectionConfig = null;
        this.peersStatus = null;
        this.participantsStatus = null;

        this.target = new EventTarget();

        this.ws = new GrabberSocket((url ?? "") + "/ws/player/" + (mode === "play" ? "play" : "admin"));
        this._setupWS();
    }

    _setupWS() {
        const _client = this;
        this.ws.on("auth:request", () => {
            _client.target.dispatchEvent(new CustomEvent("auth:request", {}));
        });

        _client.ws.on("auth:failed", () => {
            _client.ws.close();
            _client.target.dispatchEvent(new CustomEvent("auth:failed", {}));
        });

        _client.ws.on("init_peer", ({ initPeer: { pcConfig } }) => {
            _client.peerConnectionConfig = pcConfig;
            console.debug("WebRTCGrabber: connection initialized");
            _client.target.dispatchEvent(new CustomEvent("initialized", {}));
        });

        _client.ws.on("peers", ({ peersStatus, participantsStatus }) => {
            _client.peersStatus = peersStatus ?? [];
            _client.participantsStatus = participantsStatus ?? [];
            _client.target.dispatchEvent(new CustomEvent("peers", { detail: [ peersStatus, participantsStatus ] }));
        });

        _client.ws.on("offer_answer", async ({ offerAnswer: { peerId, answer } }) => {
            console.debug(`WebRTCGrabber: got offer_answer from ${peerId}`);
            await _client?.pc.setRemoteDescription(answer);
        });

        _client.ws.on("offer:failed", () => {
            _client.ws.close();
            _client.target.dispatchEvent(new CustomEvent("connection:failed", {}));
        });

        _client.ws.on("grabber_ice", async ({ ice: { peerId, candidate } }) => {
            console.debug(`WebRTCGrabber: got grabber_ice from ${peerId}`);
            await _client?.pc.addIceCandidate(candidate);
        });
    }

    formatPeerInfo(peerInfo) {
        return peerInfo.peerId ?? (`{${peerInfo.peerName}}`);
    }

    authorize(credential) {
        this.ws.emit("auth", { playerAuth: { credential: credential } });
    }

    connect(peerInfo, streamType, onVideoTrack) {
        const _client = this;

        const pc = new RTCPeerConnection(_client.peerConnectionConfig);
        pc.addTransceiver("video");
        pc.addTransceiver("audio");

        pc.addEventListener("track", (e) => {
            console.debug("WebRTCGrabber: got track");
            if (e.track.kind === "video" && e.streams.length > 0) {
                onVideoTrack(e.streams[0], peerInfo, streamType);
            }
        });

        pc.addEventListener("icecandidate", (event) => {
            console.debug(`WebRTCGrabber: sending ice to ${_client.formatPeerInfo(peerInfo)}`);
            _client.ws.emit("player_ice", { ice: { ...peerInfo, candidate: event.candidate } });
        });

        _client._closePeerConnection();
        _client.pc = pc;

        pc.createOffer().then(offer => {
            pc.setLocalDescription(offer);
            _client.ws.emit("offer", { offer: { ...peerInfo, offer, streamType } });
            console.debug(`WebRTCGrabber: sending offer to ${_client.formatPeerInfo(peerInfo)} ${streamType} ...`);
        });
    }

    on(eventName, callback) {
        this.target.addEventListener(eventName, e => callback(e.detail));
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
