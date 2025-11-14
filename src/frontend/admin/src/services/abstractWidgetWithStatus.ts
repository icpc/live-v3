import { useEffect } from "react";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "@shared/errors.ts";
import { ObjectSettings } from "@shared/api.ts";
import {
    ApiGetClient,
    ApiPostClient,
    createApiGet,
    createApiPost,
} from "@shared/utils.ts";
import { ErrorHandler, ReloadHandler } from "@shared/abstractWidget.ts";
import { BASE_URL_BACKEND } from "@/config.ts";

export class AbstractWidgetWithStatus<StatusType> {
    apiPath: string;
    apiGet: ApiGetClient;
    apiPost: ApiPostClient;
    errorHandler: ErrorHandler = (cause) => (e) => this.handleError(cause, e); // why it's not function?
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
        this.errorHandlers.forEach((h) => h(cause)(e));
    }

    isMessageRequireReload(url: string): boolean {
        return url.startsWith("/api/admin" + this.apiPath);
    }

    loadStatus() {
        return this.apiGet("/")
            .catch(
                this.errorHandler("Failed to load status of " + this.apiPath),
            )
            .then((j) => j as StatusType);
    }
}

// TODO: move to another file?
export function useServiceSnackbarErrorHandler<
    Service extends AbstractWidgetWithStatus<Settings>,
    Settings extends ObjectSettings,
>(service: Service) {
    const { enqueueSnackbar } = useSnackbar();
    useEffect(() => {
        const handler = errorHandlerWithSnackbar(enqueueSnackbar);
        service.addErrorHandler(handler);

        return () => {
            service.deleteErrorHandler(handler);
        };
    }, [enqueueSnackbar, service]);
}
