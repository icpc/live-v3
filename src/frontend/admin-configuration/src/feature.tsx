import React from "react";
import { Navigate, RouteObject } from "react-router-dom";
import { MenuItem } from "admin-home-page";
import ContestLog from "./components/pages/ContestInfo";
import {
    AdvancedJsonPage,
    VisualConfigPage,
    CustomFieldsPage,
    OrgCustomFieldsPage,
    SettingsJsonPage,
} from "./components/ConfigurationEditor";

export interface AdminConfigurationPage {
    name: string;
    path: string;
    element: React.ReactNode;
    adminOnly?: boolean;
}

export const adminConfigurationPages: AdminConfigurationPage[] = [
    {
        name: "Contest Info",
        path: "/contestInfo",
        element: <ContestLog />,
    },
    {
        name: "Settings JSON",
        path: "/settings",
        element: <SettingsJsonPage />,
        adminOnly: true,
    },
    {
        name: "Advanced JSON",
        path: "/advancedJson",
        element: <AdvancedJsonPage />,
        adminOnly: true,
    },
    {
        name: "Visual config",
        path: "/visualConfig",
        element: <VisualConfigPage />,
        adminOnly: true,
    },
    {
        name: "Teams custom fields",
        path: "/customFields",
        element: <CustomFieldsPage />,
        adminOnly: true,
    },
    {
        name: "Org custom fields",
        path: "/orgCustomFields",
        element: <OrgCustomFieldsPage />,
        adminOnly: true,
    },
];

export function getVisibleAdminConfigurationPages(
    isAdmin = false,
): AdminConfigurationPage[] {
    return adminConfigurationPages.filter((page) => isAdmin || !page.adminOnly);
}

export function getAdminConfigurationMenuItems(
    hiddenPageNames: string[] = [],
    isAdmin = false,
): MenuItem[] {
    return getVisibleAdminConfigurationPages(isAdmin)
        .filter((page) => !hiddenPageNames.includes(page.name))
        .map(({ name, path }) => ({ name, path }));
}

export function getAdminConfigurationRoutes(isAdmin = false): RouteObject[] {
    return [
        {
            path: "/",
            element: <Navigate to="/contestInfo" />,
        },
        ...getVisibleAdminConfigurationPages(isAdmin).map(
            ({ path, element }) => ({
                path,
                element,
            }),
        ),
    ];
}

export function isAdminConfigurationPath(pathname: string): boolean {
    return adminConfigurationPages.some((page) => pathname === page.path);
}
