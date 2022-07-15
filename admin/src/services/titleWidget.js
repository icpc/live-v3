import { PresetWidgetService } from "./presetWidget";

export class TitleWidgetService extends PresetWidgetService {
    getTemplates() {
        return this.apiGet("/templates").catch(this.errorHandler("Failed to load list of templates"));
    }
}
