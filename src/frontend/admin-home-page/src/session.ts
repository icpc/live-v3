import { MenuItem } from "./menuConfig";

export interface AdminSession {
    isAdmin: boolean;
    loggedIn: boolean;
    username: string | null;
    authEnabled: boolean;
}

export const UNKNOWN_SESSION: AdminSession = {
    isAdmin: false,
    loggedIn: false,
    username: null,
    authEnabled: true,
};

export const parseAdminSession = (response: unknown): AdminSession => {
    const raw = (response ?? {}) as Record<string, unknown>;
    return {
        isAdmin: raw.isAdmin === true,
        loggedIn: raw.loggedIn === true,
        username: typeof raw.username === "string" ? raw.username : null,
        authEnabled: raw.authEnabled !== false,
    };
};

export const getAuthMenuItem = (
    session: AdminSession | null,
    backendRoot: string,
): MenuItem | null => {
    if (session === null) {
        return null;
    }
    if (!session.authEnabled) {
        return { name: "Auth disabled", path: "", disabled: true };
    }
    const redirectTo = encodeURIComponent(window.location.pathname);
    return session.loggedIn
        ? {
              name: `Logout${session.username ? ` (${session.username})` : ""}`,
              path: `${backendRoot}/logout?redirectTo=${redirectTo}`,
              external: true,
          }
        : {
              name: "Login",
              path: `${backendRoot}/login?redirectTo=${redirectTo}`,
              external: true,
          };
};
