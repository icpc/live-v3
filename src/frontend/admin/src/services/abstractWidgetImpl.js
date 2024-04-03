import { AbstractWidgetService } from "shared-code/abstractWidget";
import { BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL } from "../config";

export class AbstractWidgetImpl extends AbstractWidgetService {
    constructor(apiPath, errorHandler, listenWS) {
        super(BASE_URL_BACKEND, ADMIN_ACTIONS_WS_URL, apiPath, errorHandler, listenWS);

    }
}
