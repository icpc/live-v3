import { useMemo } from "react";
import { PresetWidgetService } from "./presetWidget";
import { ErrorHandler } from "@shared/abstractWidget";

export interface TitleTemplate {
    id: string | number;
    name: string;
    content: string;
    [key: string]: unknown;
}

export class TitleWidgetService<S = Record<string, unknown>> extends PresetWidgetService<S> {
    constructor(apiPath: string, errorHandler: ErrorHandler, listenWS: boolean = true) {
        super(apiPath, errorHandler, listenWS);
    }

    async getTemplates(): Promise<TitleTemplate[]> {
        try {
            return await this.apiGet("/templates");
        } catch (error) {
            throw this.errorHandler("Failed to load list of templates")(error);
        }
    }
}

export const useTitleWidgetService = <S = Record<string, unknown>>(
    apiPath: string,
    errorHandler: ErrorHandler,
    listenWS: boolean = true
): TitleWidgetService<S> => {
    return useMemo(
        () => new TitleWidgetService<S>(apiPath, errorHandler, listenWS),
        [apiPath, errorHandler, listenWS]
    );
};
