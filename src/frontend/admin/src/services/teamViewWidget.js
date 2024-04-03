import { AbstractWidgetService } from "shared-code/abstractWidget";
import { useMemo } from "react";
import { BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL } from "../config";

const getTeamViewVariantParams = (variant) => {
    switch (variant) {
    case "splitScreen":
        return [["TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"], "splitScreen"];
    case "pvp":
        return [["PVP_TOP", "PVP_BOTTOM"], "teamPVP"];
    default:
        return [[null], "teamView"];
    }
};

export class TeamViewService extends AbstractWidgetService {
    constructor(variant, errorHandler, listenWS = true) {
        const [instances, apiPath] = getTeamViewVariantParams(variant);
        super(BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL, "/" + apiPath, errorHandler, listenWS);
        this.variant = variant;
        this.instances = instances;
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
}

export const useTeamViewService = (variant, errorHandler, listenWS) =>
    useMemo(
        () => new TeamViewService(variant, errorHandler, listenWS),
        [variant, errorHandler, listenWS]);
