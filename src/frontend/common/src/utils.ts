export type ApiPostClient = (
    path: string,
    body?: any,
    method?: string,
    rawText?: boolean,
) => Promise<any>;

const parseApiResponse = async (response: Response) => {
    const text = await response.text();
    let parsed;
    try {
        parsed = text ? JSON.parse(text) : undefined;
    } catch {
        const raw = text.trim().slice(0, 300);
        throw new Error(
            `Server returned ${response.status} ${response.statusText}` +
                (raw ? `: ${raw}` : ""),
        );
    }
    if (parsed?.status !== "ok") {
        throw new Error(
            parsed?.message ??
                `Server returned ${response.status} ${response.statusText}`,
        );
    }
    return parsed;
};

export const createApiPost: (apiUrl: string) => ApiPostClient = (apiUrl) => {
    return (path, body = {}, method = "POST", sendRaw = false) => {
        const isFormData = body instanceof FormData;
        const requestOptions: RequestInit = {
            method: method,
            // FormData sets its own Content-Type, as only it knows the multipart boundary.
            headers: isFormData
                ? undefined
                : {
                      "Content-Type": sendRaw
                          ? "text/plain"
                          : "application/json",
                  },
            body:
                method === "GET"
                    ? undefined
                    : isFormData || sendRaw
                      ? body
                      : JSON.stringify(body),
        };
        return fetch(apiUrl + path, requestOptions).then(parseApiResponse);
    };
};

export type ApiGetClient = (
    path: string,
    body?: any,
    rawText?: boolean,
) => Promise<any>;

export const createApiGet: (apiUrl: string) => ApiGetClient = (apiUrl) => {
    return (path, body = undefined, rawText = false) => {
        const requestOptions = {
            headers: { "Content-Type": "application/json" },
            body: body !== undefined ? JSON.stringify(body) : undefined,
        };
        return fetch(apiUrl + path, requestOptions).then((response) =>
            rawText ? response.text() : response.json(),
        );
    };
};
