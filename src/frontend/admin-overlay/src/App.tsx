import { useState, useCallback } from "react";
import "./App.css";
import AppNav from "./AppNav.tsx";
import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { Overlay } from "./components/Overlay.tsx";
import { SnackbarProvider } from "notistack";
import { useLocalStorageState } from "./utils";
import { ThemeProvider } from "@mui/material";
import { BASE_URL_BACKEND } from "./config";
import {
    ReloadHandleContext,
    useReloadHandleService,
} from "@admin/services/reloadHandler.ts";
import {
    AdminHomePage,
    AdminLayout,
    createAdminTheme,
    useAdminBranding,
} from "admin-home-page";
import {
    adminOverlayPages,
    getAdminOverlaySectionMenuItems,
} from "./navigation";
import {
    adminConfigurationPages,
    AdminConfigurationAppNav,
    getAdminConfigurationMenuItems,
    isAdminConfigurationPath,
} from "admin-configuration";

export function AdminApp() {
    const [isOverlayPreviewShown, setIsOverlayPreviewShown] =
        useLocalStorageState("OverlayPreviewShown", false);

    const { contestColor, hiddenPageNames } = useAdminBranding(
        BASE_URL_BACKEND,
        "ICPC Live 3 Admin",
    );

    const reloadHandleService = useReloadHandleService();

    const handleToggleOverlayPreview = useCallback(() => {
        setIsOverlayPreviewShown(!isOverlayPreviewShown);
    }, [isOverlayPreviewShown, setIsOverlayPreviewShown]);

    const [drawerOpen, setDrawerOpen] = useState(false);

    const handleDrawerToggle = useCallback(() => {
        setDrawerOpen((open) => !open);
    }, []);
    const location = useLocation();

    const menuItems = [
        ...getAdminOverlaySectionMenuItems(hiddenPageNames),
        ...(getAdminConfigurationMenuItems(hiddenPageNames).length > 0
            ? [{ name: "Configuration", path: "/contestInfo" }]
            : []),
    ];
    const isRootSection = location.pathname === "/";
    const isConfigurationSection = isAdminConfigurationPath(location.pathname);

    return (
        <AdminLayout
            hideToggleButton
            drawerOpen={drawerOpen}
            onDrawerToggle={handleDrawerToggle}
            menuItems={menuItems}
        >
            <ReloadHandleContext.Provider value={reloadHandleService}>
                <SnackbarProvider maxSnack={5}>
                    <div className="App">
                        <ThemeProvider theme={createAdminTheme(contestColor)}>
                            {isRootSection ? null : isConfigurationSection ? (
                                <AdminConfigurationAppNav
                                    onDrawerToggle={handleDrawerToggle}
                                    hiddenPageNames={hiddenPageNames}
                                />
                            ) : (
                                <AppNav
                                    showOrHideOverlayPreview={
                                        handleToggleOverlayPreview
                                    }
                                    onDrawerToggle={handleDrawerToggle}
                                    hiddenPageNames={hiddenPageNames}
                                />
                            )}
                        </ThemeProvider>
                        <Routes>
                            <Route path="/" element={<AdminHomePage />} />
                            {adminOverlayPages.map((page) => (
                                <Route
                                    key={page.path}
                                    path={page.path}
                                    element={page.element}
                                />
                            ))}
                            {adminConfigurationPages.map((page) => (
                                <Route
                                    key={page.path}
                                    path={page.path}
                                    element={page.element}
                                />
                            ))}
                        </Routes>
                        <Overlay
                            isOverlayPreviewShown={isOverlayPreviewShown}
                        />
                    </div>
                </SnackbarProvider>
            </ReloadHandleContext.Provider>
        </AdminLayout>
    );
}

function App() {
    return (
        <BrowserRouter basename={import.meta.env.BASE_URL ?? ""}>
            <AdminApp />
        </BrowserRouter>
    );
}

export default App;
