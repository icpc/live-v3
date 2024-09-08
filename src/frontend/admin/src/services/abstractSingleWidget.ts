import { useMemo, useState, useEffect, useCallback } from "react";
import { ObjectSettings } from "@shared/api.ts";
import { AbstractWidgetWithStatus, useServiceSnackbarErrorHandler } from "@/services/abstractWidgetWithStatus.ts";
import { useReloadHandler } from "@/services/reloadHandler.ts";

export interface ObjectStatus<SettingsType extends ObjectSettings> {
    shown: boolean;
    settings: SettingsType
}

export class AbstractSingleWidgetService<Settings extends ObjectSettings> extends AbstractWidgetWithStatus<ObjectStatus<Settings>> {
    constructor(apiPath: string) {
        super(apiPath);
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
