import { ApiGetClient, ApiPostClient, createApiGet, createApiPost } from "@shared/utils";
import { BASE_URL_BACKEND } from "@/config.ts";
import { useReloadHandler } from "@/services/reloadHandler.ts";
import { ErrorHandler, ReloadHandler } from "@shared/abstractWidget.ts";
import { ObjectSettings } from "@shared/api.ts";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "@shared/errors.ts";

export interface ObjectStatus<SettingsType extends ObjectSettings> {
    shown: boolean;
    settings: SettingsType
}

export class AbstractSingleWidgetService<Settings extends ObjectSettings> {
    apiPath: string;
    apiGet: ApiGetClient;
    apiPost: ApiPostClient;
    errorHandler: ErrorHandler = cause => e => this.handleError(cause, e); // why it's not function?
    errorHandlers: Set<ErrorHandler> = new Set();
    reloadDataHandlers: Set<ReloadHandler> = new Set();
    ws?: WebSocket;

    constructor(apiPath: string) {
        this.apiPath = apiPath;
        this.apiGet = createApiGet(BASE_URL_BACKEND + apiPath);
        this.apiPost = createApiPost(BASE_URL_BACKEND + apiPath);
    }

    addReloadDataHandler(handler: ReloadHandler) {
        this.reloadDataHandlers.add(handler);
    }

    deleteReloadDataHandler(handler: ReloadHandler) {
        this.reloadDataHandlers.delete(handler);
    }

    addErrorHandler(handler: ErrorHandler) {
        this.errorHandlers.add(handler);
    }

    deleteErrorHandler(handler: ErrorHandler) {
        this.errorHandlers.delete(handler);
    }

    handleError(cause: string, e: Error) {
        if (this.errorHandlers.size === 0) {
            console.error(cause + ": " + e);
        }
        this.errorHandlers.forEach(h => h(cause)(e));
    }

    isMessageRequireReload(url: string): boolean {
        return url.startsWith("/api/admin" + this.apiPath);
    }

    loadStatus() {
        return this.apiGet("/")
            .catch(this.errorHandler("Failed to load status of " + this.apiPath))
            .then(j => j as ObjectStatus<Settings>);
    }

    show() {
        return this.apiPost("/show").catch(this.errorHandler("Failed to show " + this.apiPath));
    }

    hide() {
        return this.apiPost("/hide").catch(this.errorHandler("Failed to hide " + this.apiPath));
    }

    showWithSettings(settings: Settings) {
        return this.apiPost("/show_with_settings", settings).catch(this.errorHandler("Failed to show " + this.apiPath));
    }

    setSettings(settings: Settings) {
        return this.apiPost("/", settings).catch(this.errorHandler("Failed to set settings " + this.apiPath));
    }
}

// TODO: move to another file?
export function useServiceSnackbarErrorHandler<
    Service extends AbstractSingleWidgetService<Settings>,
    Settings extends ObjectSettings
>(service: Service) {
    const { enqueueSnackbar, } = useSnackbar();
    useEffect(() => {
        const handler = errorHandlerWithSnackbar(enqueueSnackbar);
        service.addErrorHandler(handler);

        return () => {
            service.deleteErrorHandler(handler);
        };
    }, [enqueueSnackbar, service]);
}

export function useSingleWidgetService<Settings extends ObjectSettings>(apiPath: string) {
    const service = useMemo(
        () => new AbstractSingleWidgetService<Settings>(apiPath),
        [apiPath]);
    useServiceSnackbarErrorHandler(service);

    return service;
}

export function useServiceLoadStatus<
    Service extends AbstractSingleWidgetService<Settings>,
    Settings extends ObjectSettings
>(service: Service, defaultSettings: Settings) {
    const [isShown, setIsShown] = useState<boolean>(false);
    const [settings, setSettings] = useState<Settings>(defaultSettings);

    const loadStatus = useCallback(() => {
        service.loadStatus().then(s => {
            setIsShown(s.shown);
            setSettings(s.settings);
        });
    }, [service, setIsShown, setSettings]);
    useEffect(() => {
        loadStatus();
    }, [loadStatus]);

    const reloadHandler = useReloadHandler();
    useEffect(() => {
        const handler = (path: string) => service.isMessageRequireReload(path) && loadStatus();
        reloadHandler.subscribe(handler);

        return () => reloadHandler.unsubscribe(handler);
    }, [reloadHandler, service, loadStatus]);

    return { isShown, settings, setSettings, loadStatus };
}
