import { ADMIN_ACTIONS_WS_URL, BASE_URL_BACKEND } from "../config";
import { createApiGet, createApiPost } from "../utils";

export class AbstractWidgetService {
    constructor(apiPath, errorHandler, listenWS) {
        this.apiPath = apiPath;
        this.apiUrl = BASE_URL_BACKEND + apiPath;
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = errorHandler ?? (cause => e => console.error(cause + ": " + e));
        if (listenWS) {
            this.ws = new WebSocket(ADMIN_ACTIONS_WS_URL);
            this.ws.onmessage = ({ data }) => this.isMessageRequireReload(data) && this.reloadDataHandlers.forEach(h => h(data));
        }
        this.reloadDataHandlers = [];
    }

    setReloadDataHandler(handler) {
        this.reloadDataHandlers.push(handler);
    }

    isMessageRequireReload(/* data */) {}

    loadPresets() {}

    createPreset(/* presetSettings */) {}

    editPreset(/* presetId, presetSettings */) {}

    deletePreset(/* presetId */) {}

    showPreset(presetId) {
        return this.apiPost("/" + presetId + "/show").catch(this.errorHandler("Failed to show preset"));
    }

    hidePreset(presetId) {
        return this.apiPost("/" + presetId + "/hide").catch(this.errorHandler("Failed to hide preset"));
    }
}
