export interface MenuItem {
    name: string;
    path: string;
}

export const ADMIN_MENU_ITEMS: MenuItem[] = [
    {
        name: "Overlay frontend",
        path: "/admin",
    },
];

export const DEFAULT_MENU_ITEM = ADMIN_MENU_ITEMS[0];
