import React from "react";
import { Navigate, RouteObject } from "react-router-dom";
import { MenuItem } from "admin-home-page";
import ContestLog from "./components/pages/ContestInfo";
import {
    AdvancedJsonPage,
    VisualConfigPage,
    CustomFieldsPage,
    OrgCustomFieldsPage,
} from "./components/ConfigurationEditor";

export interface AdminConfigurationPage {
    name: string;
    path: string;
    element: React.ReactNode;
}

export const adminConfigurationPages: AdminConfigurationPage[] = [
    {
        name: "Contest Info",
        path: "/contestInfo",
        element: <ContestLog />,
    },
    {
        name: "Advanced JSON",
        path: "/advancedJson",
        element: <AdvancedJsonPage />,
    },
    {
        name: "Visual config",
        path: "/visualConfig",
        element: <VisualConfigPage />,
    },
    {
        name: "Teams custom fields",
        path: "/customFields",
        element: <CustomFieldsPage />,
    },
    {
        name: "Org custom fields",
        path: "/orgCustomFields",
        element: <OrgCustomFieldsPage />,
    },
];

export function getAdminConfigurationMenuItems(
    hiddenPageNames: string[] = [],
): MenuItem[] {
    return adminConfigurationPages
        .filter((page) => !hiddenPageNames.includes(page.name))
        .map(({ name, path }) => ({ name, path }));
}

export function getAdminConfigurationRoutes(): RouteObject[] {
    return [
        {
            path: "/",
            element: <Navigate to="/contestInfo" />,
        },
        ...adminConfigurationPages.map(({ path, element }) => ({
            path,
            element,
        })),
    ];
}

export function isAdminConfigurationPath(pathname: string): boolean {
    return adminConfigurationPages.some((page) => pathname === page.path);
}
