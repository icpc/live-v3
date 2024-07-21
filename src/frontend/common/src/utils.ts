export type ApiPostClient = (path: string, body?: any, method?: string, rawText?: boolean) => Promise<any>

export const createApiPost: (apiUrl: string) => ApiPostClient = (apiUrl) => {
    return (path, body = {}, method = "POST", sendRaw = false) => {
        const requestOptions = {
            method: method,
            headers: {"Content-Type": sendRaw ? "text/plain" : "application/json"},
            body: method === "GET" ? undefined : (sendRaw ? body : JSON.stringify(body)),
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => response.json())
            .then(response => {
                if (response.status !== "ok") {
                    throw new Error("Server return not ok status: " + response);
                }
                return response;
            });
    };
};

export type ApiGetClient = (path: string, body?: any, rawText?: boolean) => Promise<any>

export const createApiGet: (apiUrl: string) => ApiGetClient = (apiUrl) => {
    return (path, body = undefined, rawText = false) => {
        const requestOptions = {
            headers: {"Content-Type": "application/json"},
            body: body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => rawText ? response.text() : response.json());
    };
};
