import { AbstractWidgetService, ErrorHandler } from "@shared/abstractWidget";
import { BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL } from "../config";

export class AbstractWidgetImpl<PresetSettings> extends AbstractWidgetService<PresetSettings> {
    constructor(apiPath: string, errorHandler: ErrorHandler, listenWS: boolean) {
        super(BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL, apiPath, errorHandler, listenWS);

    }
}
