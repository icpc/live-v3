import { backendHost } from "./consts.js";
import { WebSocket } from "ws";
import { WidgetSetting } from "./types.js";
import { request, APIRequestContext } from "@playwright/test";

export class BackendClient {
    baseURL: string;
    wsURL: string;
    adminApiContext: APIRequestContext;

    constructor(port: number) {
        this.baseURL = `http://${backendHost}:${port}`;
        this.wsURL = `ws://${backendHost}:${port}`;
    }

    async buildContext() {
        this.adminApiContext = await request.newContext({
            baseURL: `${this.baseURL}/api/admin/`
        });
    }

    finalizedContestInfo() {
        const contestInfo = new WebSocket(`${this.wsURL}/api/overlay/contestInfo`);

        return new Promise((resolve) => {
            contestInfo.onmessage = (event) => {
                const message = JSON.parse(event.data.toString());
                if (message.status === "OVER" || message.status == "FINALIZED") {
                    contestInfo.close();
                    resolve(message);
                }
            };
        });
    };

    async showWidget(widget: WidgetSetting) {
        const showWidget = await this.adminApiContext.post(`./${widget.path}/show_with_settings`, {
            data: widget.settings ?? {},
        });
        return showWidget.ok();
    }

    async hideWidget(widget: WidgetSetting) {
        const showWidget = await this.adminApiContext.post(`./${widget.path}/hide`);
        return showWidget.ok();
    }

    async makeFeatured(messageId: string, mediaType: string) {
        const showWidget = await this.adminApiContext.post(`./analytics/${messageId}/featuredRun`, {
            data: mediaType,
            headers: { "Content-Type": "application/json" }
        });
        return showWidget.ok();
    }

    async hideFeatured(messageId: string) {
        const showWidget = await this.adminApiContext.delete(`./analytics/${messageId}/featuredRun`);
        return showWidget.ok();
    }
}
