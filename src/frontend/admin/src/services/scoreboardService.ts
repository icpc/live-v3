import { useMemo } from "react";
import { GroupInfo, ScoreboardSettings } from "@shared/api.ts";
import { AbstractSingleWidgetService } from "@/services/abstractSingleWidget.ts";
import { useServiceSnackbarErrorHandler } from "@/services/abstractWidgetWithStatus.ts";

export class ScoreboardWidgetService extends AbstractSingleWidgetService<ScoreboardSettings> {
    constructor() {
        super("/scoreboard");
    }

    groups() {
        return this.apiGet("/regions")
            .catch(
                this.errorHandler(
                    "Failed to load list of groups of " + this.apiPath,
                ),
            )
            .then((r) => r as GroupInfo[]);
    }
}

export function useScoreboardWidgetService() {
    const service = useMemo(() => new ScoreboardWidgetService(), []);
    useServiceSnackbarErrorHandler(service);

    return service;
}
