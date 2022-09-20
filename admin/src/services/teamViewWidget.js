import { AbstractWidgetService } from "./abstractWidget";
import { useMemo } from "react";

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
        super("/" + apiPath, errorHandler, listenWS);
        this.instances = instances;
    }

    isMessageRequireReload(data) {
        return data.startsWith("/api/admin" + this.apiPath);
    }

    presetSubPath(presetId) {
        return presetId == null ? "" : "/" + presetId;
    }

    loadOne(element) {
        return this.apiGet(this.presetSubPath(element)).catch(this.errorHandler("Failed to load " + element + " info"));
    }

    loadElements() {
        return Promise.all(
            this.instances.map(id =>
                this.loadOne(id).then(r => [id, r])))
            .then(els => els.reduce((s, el) => ({ ...s, [el[0]]: el[1] }), {}));
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
