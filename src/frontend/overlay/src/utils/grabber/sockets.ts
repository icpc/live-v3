interface MessageData {
    event: string;
    [key: string]: unknown;
}

export class GrabberSocket {
    private url: string;
    private target: EventTarget;
    private messageQueue: MessageData[];
    private isClosed: boolean;
    private ws!: WebSocket;

    constructor(url: string) {
        if (url.startsWith("http")) {
            url = "ws" + url.substring(4);
        } else if (!url.startsWith("ws")) {
            url =
                (window.location.protocol === "http:" ? "ws:" : "wss:") +
                window.location.host +
                url;
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

        ws.onopen = () => {
            while (this.messageQueue.length > 0) {
                ws.send(JSON.stringify(this.messageQueue[0]));
                this.messageQueue.splice(0, 1);
            }
        };

        ws.onmessage = ({ data }) => {
            const payload = JSON.parse(data);
            this.target.dispatchEvent(
                new CustomEvent(payload.event, { detail: payload }),
            );
        };

        ws.onclose = () => {
            if (this.isClosed) {
                return;
            }
            // setTimeout(() => this.connect(), 1000);
        };

        this.ws = ws;
    }

    emit(event: string, payload: Record<string, unknown>) {
        const data = { ...payload, event: event };
        if (this.ws.readyState === this.ws.OPEN) {
            this.ws.send(JSON.stringify(data));
        } else {
            this.messageQueue.push(data);
        }
    }

    on(event: string, callback: (detail: unknown) => void) {
        this.target.addEventListener(event, (e) =>
            callback((e as CustomEvent).detail),
        );
    }

    close() {
        this.isClosed = true;
        this.ws?.close();
    }
}
