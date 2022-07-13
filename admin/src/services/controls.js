import { createApiGet, createApiPost } from "../utils";
import { ADMIN_ACTIONS_WS_URL, BASE_URL_BACKEND } from "../config";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" }];

export class ControlsService {
    constructor(errorHandler) {
        this.apiUrl = BASE_URL_BACKEND;
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = errorHandler ?? (() => {});
        this.ws = new WebSocket(ADMIN_ACTIONS_WS_URL);
        this.ws.onmessage = ({ data }) =>
            controlElements.some(({ id }) => data.startsWith("/api/admin/" + id)) ? this.reloadDataHandler?.() : undefined;
    }

    setReloadDataHandler(handler) {
        this.reloadDataHandler = handler;
    }

    loadOne(element) {
        return this.apiGet("/" + element).catch((e) => this.errorHandler("Failed to load " + element + " info")(e));
    }

    loadAll() {
        return Promise.all(
            controlElements.map(({ id, text }) =>
                this.loadOne(id).then(r => ({ id: id, settings: { text: text }, shown: r.shown }))));
    }

    showPreset(presetId) {
        return this.apiPost("/" + presetId + "/show").catch((e) => this.errorHandler("Failed to show preset")(e));
    }

    hidePreset(presetId) {
        return this.apiPost("/" + presetId + "/hide").catch((e) => this.errorHandler("Failed to hide preset")(e));
    }
}
