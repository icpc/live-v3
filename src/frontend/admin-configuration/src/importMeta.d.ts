interface ImportMetaEnv {
    readonly VITE_BACKEND_PORT?: string;
    readonly VITE_BACKEND_ROOT?: string;
    readonly VITE_BACKEND_URL?: string;
    readonly VITE_OVERLAY_LOCATION?: string;
    readonly VITE_MEDIAS_LOCATION?: string;
    readonly VITE_WEBSOCKET_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

declare module "*.svg?raw" {
    const content: string;
    export default content;
}
