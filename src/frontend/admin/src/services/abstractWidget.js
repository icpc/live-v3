import { ADMIN_ACTIONS_WS_URL, BASE_URL_BACKEND } from "../config";
import { createApiGet, createApiPost } from "../utils";

const WEBSOCKET_RECONNECT_TIME = 3000;

export class AbstractWidgetService {
    constructor(apiPath, errorHandler, listenWS) {
        this.apiPath = apiPath;
        this.apiUrl = BASE_URL_BACKEND + apiPath;
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

    isMessageRequireReload(/* data */) {}

    loadPresets() {}

    createPreset(/* presetSettings */) {}

    editPreset(/* presetId, presetSettings */) {}

    deletePreset(/* presetId */) {}

    presetSubPath(presetId) {
        return "/" + presetId;
    }

    showPreset(presetId) {
        console.log("hey", this, this.apiPost);
        return this.apiPost(this.presetSubPath(presetId) + "/show").catch(this.errorHandler("Failed to show preset"));
    }

    showPresetWithSettings(presetId, settings) {
        return this.apiPost(this.presetSubPath(presetId) + "/show_with_settings", settings).catch(this.errorHandler("Failed to show preset"));
    }

    hidePreset(presetId) {
        return this.apiPost(this.presetSubPath(presetId) + "/hide").catch(this.errorHandler("Failed to hide preset"));
    }
}
