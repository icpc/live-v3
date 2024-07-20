export const createApiPost = (apiUrl: string) => {
    return (path: string, body: any = {}, method: string = "POST", sendRaw: boolean = false) => {
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

export const createApiGet = (apiUrl: string) => {
    return (path: string, body: any = undefined, rawText: boolean = false) => {
        const requestOptions = {
            headers: {"Content-Type": "application/json"},
            body: body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => rawText ? response.text() : response.json());
    };
};
