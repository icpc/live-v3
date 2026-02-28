import React from "react";
import { MenuItem } from "admin-home-page";
import TickerMessage from "./components/TickerMessage";
import ControlsPage from "./components/pages/ControlsPage";
import Advertisement from "./components/Advertisement";
import Title from "./components/Title";
import Picture from "./components/Picture";
import TeamView from "./components/pages/TeamView";
import BackendLog from "./components/BackendLog";
import Dashboard from "./components/Dashboard";
import Analytics from "./components/pages/Analytics";
import TeamSpotlight from "./components/TeamSpotlight";
import MediaFiles from "./components/MediaFiles";
import ScoreboardPage from "./components/pages/ScoreboardPage";

export interface AdminOverlayPage {
    name: string;
    path: string;
    element: React.ReactNode;
}

const titleElements = {
    Advertisement: <Advertisement />,
    Title: <Title />,
    Picture: <Picture />,
};

export const adminOverlayPages: AdminOverlayPage[] = [
    {
        name: "Controls",
        path: "/controls",
        element: <ControlsPage />,
    },
    {
        name: "Titles",
        path: "/titles",
        element: (
            <Dashboard
                elements={titleElements}
                layout="oneColumn"
                maxWidth="lg"
            />
        ),
    },
    {
        name: "TeamView",
        path: "/teamview",
        element: <TeamView />,
    },
    {
        name: "Scoreboard",
        path: "/scoreboard",
        element: <ScoreboardPage />,
    },
    {
        name: "Ticker",
        path: "/ticker",
        element: <TickerMessage />,
    },
    {
        name: "Analytics",
        path: "/analytics",
        element: <Analytics />,
    },
    {
        name: "Spotlight",
        path: "/teamSpotlight",
        element: <TeamSpotlight />,
    },
    {
        name: "Media",
        path: "/media",
        element: <MediaFiles />,
    },
    {
        name: "Backend Log",
        path: "/log",
        element: <BackendLog />,
    },
];

export function getAdminOverlayMenuItems(
    hiddenPageNames: string[] = [],
): MenuItem[] {
    return adminOverlayPages
        .filter((page) => !hiddenPageNames.includes(page.name))
        .map(({ name, path }) => ({ name, path }));
}

export function getAdminOverlaySectionMenuItems(
    hiddenPageNames: string[] = [],
): MenuItem[] {
    const items: MenuItem[] = [{ name: "Home", path: "/" }];

    if (getAdminOverlayMenuItems(hiddenPageNames).length > 0) {
        items.push({ name: "Overlay", path: "/controls" });
    }

    return items;
}
