import { AbstractWidgetService } from "./abstractWidget";
import { useMemo } from "react";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" },
    { text: "SplitScreen", id: "splitScreen" },
];

export class ControlsWidgetService extends AbstractWidgetService {
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
        return Promise.all(
            controlElements.map(({ id, text }) =>
                this.loadOne(id).then(r => ({ id: id, settings: { text: text }, shown: r.shown }))));
    }
}

export const useControlsWidgetService = (errorHandler, listenWS) => useMemo(
    () => new ControlsWidgetService(errorHandler, listenWS),
    []);
