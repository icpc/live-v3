import { AbstractWidgetImpl } from "../services/abstractWidgetImpl";
import { useMemo } from "react";
import { createApiGet } from "shared-code/utils";
import { BACKEND_ROOT } from "../config";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" },
    // { text: "SplitScreen", id: "splitScreen" },
    { text: "Full screen clock", id: "fullScreenClock" },
];

export class ControlsWidgetService extends AbstractWidgetImpl {
    constructor(errorHandler, listenWS = true) {
        super("", errorHandler, listenWS);
    }

    isMessageRequireReload(data) {
        return controlElements.some(({ id }) => data.startsWith("/api/admin/" + id));
    }

    loadOne(element) {
        return this.apiGet("/" + element).catch(this.errorHandler("Failed to load " + element + " info"));
    }

    loadPresets() {
        return createApiGet(BACKEND_ROOT)("/api/overlay/visualConfig.json")
            .then(c => {
                const hiddenElements = c["ADMIN_HIDE_CONTROL"] ?? [];
                const ce = controlElements.filter(e => !hiddenElements.includes(e.id));
                return Promise.all(
                    ce.map(({ id, text }) =>
                        this.loadOne(id).then(r => ({ id: id, settings: { text: text }, shown: r.shown }))));
            });
    }
}

export const useControlsWidgetService = (errorHandler, listenWS) => useMemo(
    () => new ControlsWidgetService(errorHandler, listenWS),
    []);
