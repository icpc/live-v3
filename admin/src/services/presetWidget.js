import { AbstractWidgetService } from "./abstractWidget";
import { useMemo } from "react";

export class PresetWidgetService extends AbstractWidgetService {
    constructor(apiPath, errorHandler, listenWS = true) {
        super(apiPath, errorHandler, listenWS);
    }

    isMessageRequireReload(data) {
        return data.startsWith("/api/admin" + this.apiPath);
    }

    loadPresets() {
        return this.apiGet("").catch(this.errorHandler("Failed to load list of presets"));
    }

    createPreset(presetSettings) {
        return this.apiPost("", presetSettings).catch(this.errorHandler("Failed to add preset"));
    }

    editPreset(presetId, presetSettings) {
        return this.apiPost("/" + presetId, presetSettings).catch(this.errorHandler("Failed to edit preset"));
    }

    deletePreset(presetId) {
        return this.apiPost("/" + presetId, {}, "DELETE").catch(this.errorHandler("Failed to delete preset"));
    }

    createAndShowWithTtl(presetSettings, ttlMs) {
        return this.apiPost("/create_and_show_with_ttl?ttl=" + ttlMs, presetSettings).catch((e) => this.errorHandler("Failed to add preset")(e));
    }

    getPreview(id) {
        return this.apiPost("/" + id + "/preview", undefined, "GET")
            .then(r => r.response)
            .catch(this.errorHandler("Failed to load preset preview"));
    }
}

export const usePresetWidgetService = (apiPath, errorHandler, listenWS) => useMemo(
    () => new PresetWidgetService(apiPath, errorHandler, listenWS),
    []);
