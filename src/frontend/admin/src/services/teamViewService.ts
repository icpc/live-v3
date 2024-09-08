import { ExternalTeamViewSettings, TeamInfo, TeamViewPosition, WidgetUsageStatisticsEntry } from "@shared/api.ts";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AbstractWidgetWithStatus, useServiceSnackbarErrorHandler } from "@/services/abstractWidgetWithStatus.ts";
import { ObjectStatus } from "./abstractSingleWidget";
import { useReloadHandler } from "./reloadHandler";

export type TeamViewContentType = "single" | "pvp" | "split";

const contentTypeApiPath: { [key in TeamViewContentType]: string; } = {
    "single": "/teamView",
    "pvp": "/teamPVP",
    "split": "/splitScreen",
};

export class TeamViewWidgetService extends AbstractWidgetWithStatus<ExternalTeamViewSettings> {
    constructor(contentType: TeamViewContentType) {
        super(contentTypeApiPath[contentType]);
    }

    showWithSettings(instance: TeamViewPosition, settings: ExternalTeamViewSettings) {
        return this.apiPost(`/${instance}/show_with_settings`, settings).catch(this.errorHandler("Failed to show " + this.apiPath));
    }

    hide(instance: TeamViewPosition) {
        return this.apiPost(`/${instance}/hide`).catch(this.errorHandler("Failed to hide " + this.apiPath));
    }

    setSettings(instance: TeamViewPosition, settings: ExternalTeamViewSettings) {
        return this.apiPost(`/${instance}`, settings).catch(this.errorHandler("Failed to set settings " + this.apiPath));
    }

    showAll() {
        return this.apiPost("/show").catch(this.errorHandler("Failed to show all " + this.apiPath));
    }

    hideAll() {
        return this.apiPost("/hide").catch(this.errorHandler("Failed to hide all " + this.apiPath));
    }

    teams() {
        return this.apiGet("/teams")
            .catch(this.errorHandler("Failed to load list of groups of " + this.apiPath))
            .then(r => r as TeamInfo[]);
    }

    loadUsageStats() {
        return this.apiGet("/usage_stats")
            .catch(this.errorHandler("Failed to load usage stats of " + this.apiPath))
            .then(r => r as WidgetUsageStatisticsEntry.per_team);
    }
}

export type CommonTeamViewInstancesState = { [k in TeamViewPosition]?: ObjectStatus<ExternalTeamViewSettings> };

export function useTeamViewWidgetService(
    contentType: TeamViewContentType,
    setStatus: (m: (s: CommonTeamViewInstancesState) => CommonTeamViewInstancesState) => void,
) {
    const service = useMemo(
        () => new TeamViewWidgetService(contentType),
        [contentType]);
    useServiceSnackbarErrorHandler(service);

    const loadStatus = useCallback(() => {
        service.loadStatus().then(s => setStatus(st => ({ ...st, ...s })));
    }, [service, setStatus]);
    useEffect(() => loadStatus(),
        [loadStatus]);

    const reloadHandler = useReloadHandler();
    useEffect(() => {
        const handler = (path: string) => service.isMessageRequireReload(path) && loadStatus();
        reloadHandler.subscribe(handler);

        return () => reloadHandler.unsubscribe(handler);
    }, [reloadHandler, service, loadStatus]);

    return service;
}

const isMessageRequireReloadAnyTeamView = (url: string) => {
    return url.startsWith("/api/admin/teamView") || url.startsWith("/api/admin/teamPVP") || url.startsWith("/api/admin/splitScreen");
};

export const useTeamViewWidgetUsageStats = (service: TeamViewWidgetService) => {
    const [usageStat, setUsageStat] = useState<WidgetUsageStatisticsEntry.per_team>();
    const loadUsageStats = useCallback(() => {
        service.loadUsageStats().then(s => setUsageStat(s));
    }, [service, setUsageStat]);
    useEffect(() => loadUsageStats(),
        [loadUsageStats]);

    const reloadHandler = useReloadHandler();
    useEffect(() => {
        const handler = (path: string) => isMessageRequireReloadAnyTeamView(path) && loadUsageStats();
        reloadHandler.subscribe(handler);

        return () => reloadHandler.unsubscribe(handler);
    }, [reloadHandler, loadUsageStats]);

    return usageStat;
};
