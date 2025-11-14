import {
    ApiGetClient,
    ApiPostClient,
    createApiGet,
    createApiPost,
} from "./utils";

const WEBSOCKET_RECONNECT_TIME = 3000;

export type ErrorHandler = (cause: string) => (e: Error) => void;
export type ReloadHandler = (data: any) => void;
export type PresetId = string;

export class AbstractWidgetService<PresetSettings> {
    ADMIN_ACTIONS_WS_URL: string;
    apiPath: string;
    apiUrl: string;
    apiGet: ApiGetClient;
    apiPost: ApiPostClient;
    errorHandler: ErrorHandler; // why it's not function?
    errorHandlers: Set<ErrorHandler>;
    reloadDataHandlers: Set<ReloadHandler>;
    ws?: WebSocket;

    constructor(
        BASE_URL_BACKEND: string,
        ADMIN_ACTIONS_WS_URL: string,
        apiPath: string,
        errorHandler: ErrorHandler,
        listenWS: boolean,
    ) {
        this.ADMIN_ACTIONS_WS_URL = ADMIN_ACTIONS_WS_URL;
        this.apiPath = apiPath;
        this.apiUrl = BASE_URL_BACKEND + apiPath;
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = (cause) => (e) => this.handleError(cause, e);
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
        this.ws = new WebSocket(this.ADMIN_ACTIONS_WS_URL);
        this.ws.onmessage = ({ data }) =>
            this.isMessageRequireReload(data) &&
            this.reloadDataHandlers.forEach((h) => h(data));
        this.ws.onclose = function () {
            this.ws = null;
            setTimeout(() => this.openWS(), WEBSOCKET_RECONNECT_TIME);
        }.bind(this);
    }

    addReloadDataHandler(handler: ReloadHandler) {
        this.reloadDataHandlers.add(handler);
    }

    deleteReloadDataHandler(handler: ReloadHandler) {
        this.reloadDataHandlers.delete(handler);
    }

    addErrorHandler(handler: ErrorHandler) {
        this.errorHandlers.add(handler);
    }

    deleteErrorHandler(handler: ErrorHandler) {
        this.errorHandlers.delete(handler);
    }

    handleError(cause: string, e: Error) {
        if (this.errorHandlers.size === 0) {
            console.error(cause + ": " + e);
        }
        this.errorHandlers.forEach((h) => h(cause)(e));
    }

    isMessageRequireReload(_: string): boolean {
        return false;
    }

    loadPresets() {}

    createPreset(_: PresetSettings) {}

    editPreset(_: PresetId, __: PresetSettings) {}

    deletePreset(_: PresetId) {}

    presetSubPath(presetId: PresetId) {
        return "/" + presetId;
    }

    showPreset(presetId: PresetId) {
        return this.apiPost(this.presetSubPath(presetId) + "/show").catch(
            this.errorHandler("Failed to show preset"),
        );
    }

    showPresetWithSettings(presetId: PresetId, settings: PresetSettings) {
        return this.apiPost(
            this.presetSubPath(presetId) + "/show_with_settings",
            settings,
        ).catch(this.errorHandler("Failed to show preset"));
    }

    hidePreset(presetId: PresetId) {
        return this.apiPost(this.presetSubPath(presetId) + "/hide").catch(
            this.errorHandler("Failed to hide preset"),
        );
    }
}
