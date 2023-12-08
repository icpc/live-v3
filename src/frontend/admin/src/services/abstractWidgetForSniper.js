import { createApiPost } from "../utils";
import { BASE_URL_BACKEND } from "../config";

const WEBSOCKET_RECONNECT_TIME = 3000;
const BACKEND_PORT = 8080;
const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
const BASE_URL_WS = process.env.REACT_APP_WEBSOCKET_URL ?? (WS_PROTO + window.location.hostname + ":" + BACKEND_PORT + "/api/admin");
// const BASE_URL_BACKEND = "http://127.0.0.1:8080/api/admin";
const ADMIN_ACTIONS_WS_URL = BASE_URL_WS + "/adminActions";

const createApiGet = (apiUrl) =>
    function (path, body = undefined) {
        const requestOptions = {
            headers: { "Content-Type": "application/json", "Authorization": "Basic YWRtaW46YWRtaW4=" },
            body:  body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => response.json());
    };

export class AbstractWidgetServiceForSniper {
    constructor(apiPath, errorHandler, listenWS) {
        this.apiPath = apiPath;
        this.apiUrl = BASE_URL_BACKEND + apiPath;
        console.log(this.apiUrl);
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = cause => e => this.handleError(cause, e);
        if (listenWS) {
            this.openWS();
        }
        this.reloadDataHandlers = new Set();
        this.errorHandlers = new Set();
        if (errorHandler) {
            this.errorHandlers.add(errorHandler);
        }
    }

    openWS() {
        this.ws = new WebSocket(ADMIN_ACTIONS_WS_URL);
        this.ws.onmessage = ({ data }) => this.isMessageRequireReload(data) && this.reloadDataHandlers.forEach(h => h(data));
        this.ws.onclose = (function () {
            this.ws = null;
            setTimeout(() => this.openWS(), WEBSOCKET_RECONNECT_TIME);
        }).bind(this);
    }

    addReloadDataHandler(handler) {
        this.reloadDataHandlers.add(handler);
    }

    deleteReloadDataHandler(handler) {
        this.reloadDataHandlers.delete(handler);
    }

    addErrorHandler(handler) {
        this.errorHandlers.add(handler);
    }

    deleteErrorHandler(handler) {
        this.errorHandlers.delete(handler);
    }

    handleError(cause, e) {
        if (this.errorHandlers.size === 0) {
            console.error(cause + ": " + e);
        }
        this.errorHandlers.forEach(h => h(cause)(e));
    }

    isMessageRequireReload(/* data */) {
    }

    loadPresets() {
    }

    createPreset(/* presetSettings */) {
    }

    editPreset(/* presetId, presetSettings */) {
    }

    deletePreset(/* presetId */) {
    }

    presetSubPath(presetId) {
        return "/" + presetId;
    }

    showPreset(presetId) {
        console.log("hey", this, this.apiPost);
        return this.apiPost(this.presetSubPath(presetId) + "/show").catch(this.errorHandler("Failed to show preset"));
    }

    showPresetWithSettings(presetId, settings) {
        settings = {
            teamID: settings.teamId,
            sniperID: Number(settings.sniperID)
        };
        console.log(settings.teamID);
        return this.apiPost("/move", settings).catch(this.errorHandler("Failed to show preset"));
    }

    hidePreset(presetId) {
        return this.apiPost(this.presetSubPath(presetId) + "/hide").catch(this.errorHandler("Failed to hide preset"));
    }
}
