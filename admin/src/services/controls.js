import { createApiGet, createApiPost } from "../utils";
import { BASE_URL_BACKEND } from "../config";

const controlElements = [
    { text: "Scoreboard", id: "scoreboard" },
    { text: "Queue", id: "queue" },
    { text: "Statistics", id: "statistics" },
    { text: "Ticker", id: "ticker" }];

export class ControlsService {
    constructor(apiPath, errorHandler) {
        this.apiUrl = BASE_URL_BACKEND + apiPath;
        this.apiGet = createApiGet(this.apiUrl);
        this.apiPost = createApiPost(this.apiUrl);
        this.errorHandler = errorHandler ?? (() => {});
    }

    loadOne(element) {
        return this.apiGet("/" + element).catch((e) => this.errorHandler("Failed to load " + element + " info")(e));
    }

    loadAll() {
        return Promise.all(
            controlElements.map(({ id, text }) =>
                this.loadOne(id).then(r => ({ id: id, settings: { text: text }, shown: r.shown }))));
        // .then(elements => this.setState(state => ({
        //     ...state,
        //     dataElements: elements,
        // })));
        //                fetch(BASE_URL_BACKEND + "/" + element.id)
        //                     .then(r => r.json())
        //                     .then(r => ({ id: element.id, settings: { text: element.text }, shown: r.shown }))
    }

    showPreset(presetId) {
        return this.apiPost("/" + presetId + "/show").catch((e) => this.errorHandler("Failed to show preset")(e));
    }

    hidePreset(presetId) {
        return this.apiPost("/" + presetId + "/hide").catch((e) => this.errorHandler("Failed to hide preset")(e));
    }
}
