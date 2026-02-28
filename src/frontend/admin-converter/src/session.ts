import { useEffect, useState } from "react";
import { createApiGet } from "shared-code/utils";
import { ADMIN_BACKEND_ROOT } from "admin-home-page";

export interface AdminSessionInfo {
    loggedIn: boolean;
    username: string | null;
}

const apiGet = createApiGet(`${ADMIN_BACKEND_ROOT}/api/admin`);

const normalizeBasePath = (basePath: string | undefined) => {
    const normalizedBasePath = (basePath ?? "").replace(/\/$/, "");
    return normalizedBasePath === "" ? "" : normalizedBasePath;
};

export const getSessionRoute = () =>
    `${normalizeBasePath(import.meta.env.BASE_URL)}/session`;

export const getLoginUrl = () =>
    `/login?redirectTo=${encodeURIComponent(getSessionRoute())}`;

export const getLogoutUrl = () =>
    `/logout?redirectTo=${encodeURIComponent(getSessionRoute())}`;

export const useAdminSession = () => {
    const [session, setSession] = useState<AdminSessionInfo | null>(null);

    useEffect(() => {
        apiGet("/session")
            .then((response) => {
                setSession({
                    loggedIn: Boolean(response.loggedIn),
                    username:
                        typeof response.username === "string"
                            ? response.username
                            : null,
                });
            })
            .catch((error) => {
                console.error("Failed to load admin session info:", error);
                setSession({
                    loggedIn: false,
                    username: null,
                });
            });
    }, []);

    return session;
};
