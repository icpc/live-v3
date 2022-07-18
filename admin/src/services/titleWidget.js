import { PresetWidgetService } from "./presetWidget";
import { useMemo } from "react";

export class TitleWidgetService extends PresetWidgetService {
    getTemplates() {
        return this.apiGet("/templates").catch(this.errorHandler("Failed to load list of templates"));
    }

    getPreview(id) {
        return this.apiPost("/" + id + "/preview", undefined, "GET")
            .then(r => r.response)
            .catch(this.errorHandler("Failed to load preset preview"));
    }
}

export const useTitleWidgetService = (apiPath, errorHandler, listenWS) => useMemo(
    () => new TitleWidgetService(apiPath, errorHandler, listenWS),
    []);
