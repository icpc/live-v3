const BACKEND_PROTO = window.location.protocol === "https:" ? "https://" : "http://";
const BACKEND_PORT = process.env.REACT_APP_BACKEND_PORT ?? window.location.port;
export const BASE_URL_BACKEND = process.env.REACT_APP_BACKEND_URL ?? (BACKEND_PROTO + window.location.hostname  + ":" + BACKEND_PORT + "/api/admin");
export const OVERLAY_LOCATION = process.env.REACT_APP_OVERLAY_LOCATION ?? (BACKEND_PROTO + window.location.hostname  + ":" + BACKEND_PORT + "/overlay");

const WS_PROTO = window.location.protocol === "https:" ? "wss://" : "ws://";
export const BASE_URL_WS = process.env.REACT_APP_WEBSOCKET_URL ?? (WS_PROTO + window.location.hostname + ":" + BACKEND_PORT + "/api/admin");

export const WEBSOCKET_RECONNECT_TIME = 5000;

export const ADMIN_ACTIONS_WS_URL = BASE_URL_WS + "/adminActions";
