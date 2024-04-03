import { AbstractWidgetService } from "shared-code/abstractWidget";
import { useMemo } from "react";
import { BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL } from "../config";

export class FullScreenClockService extends AbstractWidgetService {
    constructor(errorHandler, listenWS = true) {
        super(BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL, "/fullScreenClock", errorHandler, listenWS);
    }

    isMessageRequireReload(data) {
        return data.startsWith("/api/admin" + this.apiPath);
    }

    presetSubPath(/* presetId */) {
        return "";
    }

    loadOne(element) {
        return this.apiGet(this.presetSubPath(element)).catch(this.errorHandler("Failed to load " + element + " info"));
    }

    // loadElements() {
    //     return Promise.all(
    //         this.instances.map(id =>
    //             this.loadOne(id).then(r => [id, r])))
    //         .then(els => els.reduce((s, el) => ({ ...s, [el[0]]: el[1] }), {}));
    // }

    editPreset(element, settings) {
        return this.apiPost(this.presetSubPath(element), settings).catch(this.errorHandler("Failed to edit element"));
    }
}

export const useFullScreenClockWidget = (errorHandler, listenWS) =>
    useMemo(() => new FullScreenClockService(errorHandler, listenWS),
        [errorHandler, listenWS]);
