import { useMemo } from "react";
import { AbstractWidgetImpl } from "./abstractWidgetImpl";
import { ErrorHandler } from "@shared/abstractWidget";

export interface Preset<S = Record<string, unknown>> {
    id: string | number;
    shown: boolean;
    settings: S;
}

export interface PresetPreview {
    url?: string;
    data?: unknown;
    [key: string]: unknown;
}

export interface CreateAndShowOptions {
    ttlMs: number;
}

export class PresetWidgetService<
    S = Record<string, unknown>,
> extends AbstractWidgetImpl<S> {
    constructor(
        apiPath: string,
        errorHandler: ErrorHandler,
        listenWS: boolean = true,
    ) {
        super(apiPath, errorHandler, listenWS);
    }

    isMessageRequireReload(data: string): boolean {
        return data.startsWith(`/api/admin${this.apiPath}`);
    }

    async loadPresets(): Promise<Preset<S>[]> {
        try {
            return await this.apiGet("");
        } catch (error) {
            throw this.errorHandler("Failed to load list of presets")(error);
        }
    }

    async createPreset(presetSettings: S): Promise<void> {
        try {
            await this.apiPost("", presetSettings);
        } catch (error) {
            throw this.errorHandler("Failed to add preset")(error);
        }
    }

    async editPreset(
        presetId: string | number,
        presetSettings: S,
    ): Promise<void> {
        try {
            await this.apiPost(`/${presetId}`, presetSettings);
        } catch (error) {
            throw this.errorHandler("Failed to edit preset")(error);
        }
    }

    async deletePreset(presetId: string | number): Promise<void> {
        try {
            await this.apiPost(`/${presetId}`, {}, "DELETE");
        } catch (error) {
            throw this.errorHandler("Failed to delete preset")(error);
        }
    }

    async showPreset(presetId: string | number): Promise<void> {
        try {
            await this.apiPost(`/${presetId}/show`, {});
        } catch (error) {
            throw this.errorHandler("Failed to show preset")(error);
        }
    }

    async hidePreset(presetId: string | number): Promise<void> {
        try {
            await this.apiPost(`/${presetId}/hide`, {});
        } catch (error) {
            throw this.errorHandler("Failed to hide preset")(error);
        }
    }

    async createAndShowWithTtl(
        presetSettings: S,
        options: CreateAndShowOptions,
    ): Promise<void> {
        try {
            const { ttlMs } = options;
            await this.apiPost(
                `/create_and_show_with_ttl?ttl=${ttlMs}`,
                presetSettings,
            );
        } catch (error) {
            throw this.errorHandler("Failed to add preset")(error);
        }
    }

    async getPreview(presetId: string | number): Promise<PresetPreview> {
        try {
            return await this.apiGet(`/${presetId}/preview`);
        } catch (error) {
            throw this.errorHandler("Failed to load preset preview")(error);
        }
    }

    async batchUpdatePresets(
        updates: Array<{
            id: string | number;
            settings: S;
        }>,
    ): Promise<void> {
        try {
            const promises = updates.map(({ id, settings }) =>
                this.editPreset(id, settings),
            );
            await Promise.all(promises);
        } catch (error) {
            throw this.errorHandler("Failed to batch update presets")(error);
        }
    }

    async batchDeletePresets(presetIds: (string | number)[]): Promise<void> {
        try {
            const promises = presetIds.map((id) => this.deletePreset(id));
            await Promise.all(promises);
        } catch (error) {
            throw this.errorHandler("Failed to batch delete presets")(error);
        }
    }

    async getPreset(presetId: string | number): Promise<Preset<S>> {
        try {
            return await this.apiGet(`/${presetId}`);
        } catch (error) {
            throw this.errorHandler("Failed to load preset")(error);
        }
    }

    async duplicatePreset(
        presetId: string | number,
        modifications?: Partial<S>,
    ): Promise<void> {
        try {
            const originalPreset = await this.getPreset(presetId);
            const newSettings = {
                ...originalPreset.settings,
                ...modifications,
            };
            await this.createPreset(newSettings);
        } catch (error) {
            throw this.errorHandler("Failed to duplicate preset")(error);
        }
    }
}

export const usePresetWidgetService = <S = Record<string, unknown>>(
    apiPath: string,
    errorHandler: ErrorHandler,
    listenWS: boolean = true,
): PresetWidgetService<S> => {
    return useMemo(
        () => new PresetWidgetService<S>(apiPath, errorHandler, listenWS),
        [apiPath, errorHandler, listenWS],
    );
};
