import { AbstractWidgetService } from "./abstractWidget";

export class PresetWidgetService extends AbstractWidgetService {
    constructor(apiPath, errorHandler) {
        super(apiPath, errorHandler);
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
}
