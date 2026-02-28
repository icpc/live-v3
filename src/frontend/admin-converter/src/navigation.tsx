import React from "react";
import { Navigate } from "react-router-dom";
import { MenuItem } from "admin-home-page";
import { ClicsAdmin } from "./components/pages/ClicsAdmin";
import { PCMSAdmin } from "./components/pages/PCMSAdmin";
import { IcpcAdmin } from "./components/pages/IcpcAdmin";
import { ReactionsAdmin } from "./components/pages/ReactionsAdmin";
import { LoginAdmin } from "./components/pages/LoginAdmin";
import { AdminSessionInfo } from "./session";

export interface AdminConverterPage {
    name: string;
    path: string;
    element: React.ReactNode;
}

export const adminConverterPages: AdminConverterPage[] = [
    {
        name: "Clics",
        path: "/clics",
        element: <ClicsAdmin />,
    },
    {
        name: "PCMS",
        path: "/pcms",
        element: <PCMSAdmin />,
    },
    {
        name: "ICPC",
        path: "/icpc",
        element: <IcpcAdmin />,
    },
    {
        name: "Reactions",
        path: "/reactions",
        element: <ReactionsAdmin />,
    },
    {
        name: "Login/Logout",
        path: "/session",
        element: <LoginAdmin />,
    },
];

export function getAdminConverterMenuItems(
    sessionInfo: AdminSessionInfo | null = null,
): MenuItem[] {
    return adminConverterPages.map(({ name, path }) => ({
        name:
            path === "/session"
                ? sessionInfo?.loggedIn
                    ? "Logout"
                    : "Login"
                : name,
        path,
    }));
}

export function getAdminConverterSectionMenuItems(): MenuItem[] {
    return [
        { name: "Home", path: "/" },
        { name: "Converter", path: "/clics" },
    ];
}

export const adminConverterRootRoute = {
    path: "/",
    element: <Navigate to="/clics" replace />,
};
