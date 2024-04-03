import { useMemo } from "react";
import { AbstractWidgetService } from "shared-code/abstractWidget";
import { BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL } from "../config";

export class TeamViewService extends AbstractWidgetService {
    constructor(variant, errorHandler, listenWS = false) {
        super(BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL, "", errorHandler, listenWS);
        this.variant = variant;
        this.instances = [];
    }

    isMessageRequireReload(data) {
        return data.startsWith("/api/admin" + this.apiPath);
    }

    presetSubPath(presetId) {
        return presetId == null ? "" : "/" + presetId;
    }

    loadElements() {
        if (this.variant === "single") {
            return this.apiGet("").then(r => ({ [null]: r })).catch(this.errorHandler("Failed to load status"));
        }
        return this.apiGet("").catch(this.errorHandler("Failed to load status"));
    }

    editPreset(element, settings) {
        return this.apiPost(this.presetSubPath(element), settings).catch(this.errorHandler("Failed to edit element"));
    }

    showAll() {
        this.showPreset(null);
    }

    hideAll() {
        this.hidePreset(null);
    }

    teams() {
        return this.apiGet("/teams").catch(this.errorHandler("Failed to load team list"));
    }

    oracles() {
        return this.apiGet("/oracles").catch(this.errorHandler("Failed to load oracles list"));
    }

    moveWithSettings(settings) {
        console.log(settings);
        return this.apiPost(this.presetSubPath() + "/move", settings).catch(this.errorHandler("Failed to move oracle"));
    }

    showWithSettings(settings) {
        return this.apiPost(this.presetSubPath() + "/show_with_settings", settings).catch(this.errorHandler("Failed to show locator"));
    }

    hide() {
        return this.apiPost(this.presetSubPath() + "/hide").catch(this.errorHandler("Failed to hide locator"));
    }
}

export const useLocatorService = (errorHandler, listenWS) =>
    useMemo(
        () => new TeamViewService("single", errorHandler, listenWS),
        [errorHandler, listenWS]);
