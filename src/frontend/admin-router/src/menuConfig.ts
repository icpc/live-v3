export interface MenuItem {
    name: string;
    path: string;
}

export const ADMIN_MENU_ITEMS: MenuItem[] = [
    {
        name: "Main",
        path: "/",
    },
    {
        name: "Overlay frontend",
        path: "/admin",
    },
    {
        name: "Contest Info",
        path: "/admin-configuration",
    },
];

export const DEFAULT_MENU_ITEM = ADMIN_MENU_ITEMS[0];
