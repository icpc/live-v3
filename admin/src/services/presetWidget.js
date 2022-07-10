import { createApiGet, createApiPost } from "../utils";
import { BASE_URL_BACKEND } from "../config";

export class PresetWidgetService {
    constructor(apiPath, errorHandler) {
        this.apiUrl = BASE_URL_BACKEND + apiPath;
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = errorHandler ?? (() => {});
    }

    loadPresets() {
        return this.apiGet("").catch((e) => this.errorHandler("Failed to load list of presets")(e));
    }

    createPreset(presetSettings) {
        return this.apiPost("", presetSettings).catch((e) => this.errorHandler("Failed to add preset")(e));
    }

    editPreset(presetId, presetSettings) {
        return this.apiPost("/" + presetId, presetSettings).catch((e) => this.errorHandler("Failed to edit preset")(e));
    }

    deletePreset(presetId) {
        return this.apiPost("/" + presetId, {}, "DELETE").catch((e) => this.errorHandler("Failed to delete preset")(e));
    }

    showPreset(presetId) {
        return this.apiPost("/" + presetId + "/show").catch((e) => this.errorHandler("Failed to show preset")(e));
    }

    hidePreset(presetId) {
        return this.apiPost("/" + presetId + "/hide").catch((e) => this.errorHandler("Failed to hide preset")(e));
    }

    createAndShowWithTtl(presetSettings, ttlMs) {
        return this.apiPost("/create_and_show_with_ttl?ttl=" + ttlMs, presetSettings).catch((e) => this.errorHandler("Failed to add preset")(e));
    }
}
