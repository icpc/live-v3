export class GrabberSocket {
    constructor(url) {
        if (url.startsWith("http")) {
            url = "ws" + url.substring(4);
        } else if (!url.startsWith("ws")) {
            url = (window.location.protocol === "http:" ? "ws:" : "wss:") + window.location.host + url;
        }
        this.url = url;
        this.target = new EventTarget();
        this.messageQueue = [];
        this.isClosed = false;
        this.connect();
    }

    connect() {
        if (this.isClosed) {
            return;
        }
        const ws = new WebSocket(this.url);
        const _this = this;
        ws.onopen = function () {
            while (_this.messageQueue.length > 0) {
                ws.send(JSON.stringify(_this.messageQueue[0]));
                _this.messageQueue.splice(0, 1);
            }
        };
        ws.onmessage = function ({ data }) {
            const payload = JSON.parse(data);
            _this.target.dispatchEvent(new CustomEvent(payload.event, { detail: payload }));
        };
        ws.onclose = function () {
            if (_this.isClosed) {
                return;
            }
            setTimeout(() => _this.connect(), 1000);
        };
        this.ws = ws;
    }

    emit(event, payload) {
        const data = { ...payload, "event": event };
        if (this.ws.readyState === this.ws.OPEN) {
            this.ws.send(JSON.stringify(data));
        } else {
            this.messageQueue.push(data);
        }
    }

    on(event, callback) {
        this.target.addEventListener(event, e => callback(e.detail));
    }

    close() {
        this.isClosed = true;
        this.ws?.close();
    }
}
