const BACKEND_PROTO = window.location.protocol === "https:" ? "https://" : "http://";
const BACKEND_PORT = process.env.REACT_APP_BACKEND_PORT ?? window.location.port;
export const BASE_URL_BACKEND = process.env.REACT_APP_BACKEND_URL ?? (BACKEND_PROTO + window.location.hostname  + ":" + BACKEND_PORT + "/api/admin");
