import { AbstractWidgetService } from "./abstractWidget";
import { useMemo } from "react";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" }];

export class ControlsWidgetService extends AbstractWidgetService {
    constructor(errorHandler) {
        super("", errorHandler);
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

export const useControlsWidgetService = (errorHandler) => useMemo(
    () => new ControlsWidgetService(errorHandler),
    []);
