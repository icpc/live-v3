export const createApiPost = (apiUrl) =>
    function (path, body = {}, method = "POST") {
        const requestOptions = {
            method: method,
            headers: { "Content-Type": "application/json" },
            body:  method === "GET" ? undefined : JSON.stringify(body),
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
export const createApiGet = (apiUrl) =>
    function (path, body = undefined) {
        const requestOptions = {
            headers: { "Content-Type": "application/json" },
            body:  body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions)
            .then(response => response.json());
    };
