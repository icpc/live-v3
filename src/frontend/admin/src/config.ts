const BACKEND_PROTO =
    window.location.protocol === "https:" ? "https://" : "http://";
export const BACKEND_PORT =
    import.meta.env.VITE_BACKEND_PORT ?? window.location.port;
export const BACKEND_ROOT =
    import.meta.env.VITE_BACKEND_ROOT ??
    BACKEND_PROTO + window.location.hostname + ":" + BACKEND_PORT;
export const BASE_URL_BACKEND =
    import.meta.env.VITE_BACKEND_URL ?? BACKEND_ROOT + "/api/admin";
export const OVERLAY_LOCATION =
    import.meta.env.VITE_OVERLAY_LOCATION ?? BACKEND_ROOT + "/overlay";
export const SCHEMAS_LOCATION =
    import.meta.env.VITE_SCHEMAS_LOCATION ?? BACKEND_ROOT + "/schemas";
export const EXAMPLES_LOCATION =
    import.meta.env.VITE_SCHEMAS_LOCATION ?? BACKEND_ROOT + "/examples";
export const MEDIAS_LOCATION =
    import.meta.env.VITE_MEDIAS_LOCATION ?? BACKEND_ROOT + "/media";

export const WS_PROTO =
    window.location.protocol === "https:" ? "wss://" : "ws://";
export const BASE_URL_WS =
    import.meta.env.VITE_WEBSOCKET_URL ??
    WS_PROTO + window.location.hostname + ":" + BACKEND_PORT + "/api/admin";
export const WEBSOCKET_RECONNECT_TIME = 5000;
export const ADMIN_ACTIONS_WS_URL = BASE_URL_WS + "/adminActions";
