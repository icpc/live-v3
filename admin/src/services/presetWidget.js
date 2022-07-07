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
        return this.apiGet("").catch(() => this.errorHandler("Failed to load list of presets"));
    }

    createPreset(presetSettings) {
        return this.apiPost("", presetSettings).catch(() => this.errorHandler("Failed to add preset"));
    }

    editPreset(presetId, presetSettings) {
        return this.apiPost("/" + presetId, presetSettings).catch(() => this.errorHandler("Failed to edit preset"));
    }

    deletePreset(presetId) {
        return this.apiPost("/" + presetId, {}, "DELETE").catch(() => this.errorHandler("Failed to delete preset"));
    }

    showPreset(presetId) {
        return this.apiPost("/" + presetId + "/show").catch(() => this.errorHandler("Failed to show preset"));
    }

    hidePreset(presetId) {
        return this.apiPost("/" + presetId + "/hide").catch(() => this.errorHandler("Failed to hide preset"));
    }
}
