import { PresetWidgetService } from "./presetWidget";
import { useMemo } from "react";

export class TitleWidgetService extends PresetWidgetService {
    getTemplates() {
        return this.apiGet("/templates").catch(this.errorHandler("Failed to load list of templates"));
    }
}

export const useTitleWidgetService = (apiPath, errorHandler, listenWS) => useMemo(
    () => new TitleWidgetService(apiPath, errorHandler, listenWS),
    []);
