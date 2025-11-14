import { useMemo } from "react";
import { createApiGet } from "shared-code/utils";
import { AbstractWidgetImpl } from "./abstractWidgetImpl";
import { ErrorHandler } from "@shared/abstractWidget";
import { BACKEND_ROOT } from "../config";

interface ControlElement {
    text: string;
    id: string;
}

interface ControlElementSettings {
    text: string;
}

interface ControlElementPreset {
    id: string;
    settings: ControlElementSettings;
    shown: boolean;
}

interface LoadOneResponse {
    shown: boolean;
}

const CONTROL_ELEMENTS: readonly ControlElement[] = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" },
    { text: "Full screen clock", id: "fullScreenClock" },
] as const;

export class ControlsWidgetService extends AbstractWidgetImpl<ControlElementSettings> {
    constructor(errorHandler: ErrorHandler, listenWS: boolean = true) {
        super("", errorHandler, listenWS);
    }

    isMessageRequireReload(data: string): boolean {
        return CONTROL_ELEMENTS.some(({ id }) =>
            data.startsWith(`/api/admin/${id}`),
        );
    }

    private async loadOne(elementId: string): Promise<LoadOneResponse> {
        try {
            return await this.apiGet(`/${elementId}`);
        } catch (error) {
            throw this.errorHandler(`Failed to load ${elementId} info`)(error);
        }
    }

    private async loadVisualConfig(): Promise<string[]> {
        try {
            const config = await createApiGet(BACKEND_ROOT)(
                "/api/overlay/visualConfig.json",
            );
            return config.ADMIN_HIDE_CONTROL ?? [];
        } catch (error) {
            console.warn(
                "Failed to load visual config, proceeding with all elements visible:",
                error,
            );
            return [];
        }
    }

    private filterVisibleElements(
        hiddenElementIds: string[],
    ): ControlElement[] {
        return CONTROL_ELEMENTS.filter(
            (element) => !hiddenElementIds.includes(element.id),
        );
    }

    async loadPresets(): Promise<ControlElementPreset[]> {
        try {
            const hiddenElements = await this.loadVisualConfig();

            const visibleElements = this.filterVisibleElements(hiddenElements);

            const presetPromises = visibleElements.map(async ({ id, text }) => {
                try {
                    const response = await this.loadOne(id);
                    return {
                        id,
                        settings: { text },
                        shown: response.shown,
                    };
                } catch (error) {
                    console.error(`Failed to load state for ${id}:`, error);
                    return {
                        id,
                        settings: { text },
                        shown: false,
                    };
                }
            });

            return await Promise.all(presetPromises);
        } catch (error) {
            console.error("Failed to load control presets:", error);
            throw error;
        }
    }

    getAvailableElements(): readonly ControlElement[] {
        return CONTROL_ELEMENTS;
    }

    isElementAvailable(elementId: string): boolean {
        return CONTROL_ELEMENTS.some((element) => element.id === elementId);
    }
}

export const useControlsWidgetService = (
    errorHandler: ErrorHandler,
    listenWS: boolean = true,
): ControlsWidgetService => {
    return useMemo(
        () => new ControlsWidgetService(errorHandler, listenWS),
        [errorHandler, listenWS],
    );
};
