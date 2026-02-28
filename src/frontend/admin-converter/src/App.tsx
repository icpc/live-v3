import { useState } from "react";
import "./App.css";
import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import {
    ADMIN_BACKEND_ROOT,
    AdminHomePage,
    AdminLayout,
    createAdminTheme,
    useAdminBranding,
} from "admin-home-page";
import { ThemeProvider } from "@mui/material";
import AppNav from "./AppNav";
import {
    adminConverterPages,
    getAdminConverterSectionMenuItems,
} from "./navigation";
import {
    adminConfigurationPages,
    getAdminConfigurationMenuItems,
    AdminConfigurationAppNav,
    isAdminConfigurationPath,
} from "admin-configuration";

export function AdminApp() {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const { contestColor, hiddenPageNames } = useAdminBranding(
        `${ADMIN_BACKEND_ROOT}/api/admin`,
        "CDS Converter Admin",
    );
    const location = useLocation();

    const handleDrawerToggle = () => {
        setDrawerOpen((open) => !open);
    };
    const isRootSection = location.pathname === "/";
    const isConfigurationSection = isAdminConfigurationPath(location.pathname);

    return (
        <AdminLayout
            hideToggleButton
            drawerOpen={drawerOpen}
            onDrawerToggle={handleDrawerToggle}
            menuItems={[
                ...getAdminConverterSectionMenuItems(),
                ...(getAdminConfigurationMenuItems(hiddenPageNames).length > 0
                    ? [{ name: "Configuration", path: "/contestInfo" }]
                    : []),
            ]}
        >
            <div className="App">
                <ThemeProvider theme={createAdminTheme(contestColor)}>
                    {isRootSection ? null : isConfigurationSection ? (
                        <AdminConfigurationAppNav
                            onDrawerToggle={handleDrawerToggle}
                            hiddenPageNames={hiddenPageNames}
                        />
                    ) : (
                        <AppNav onDrawerToggle={handleDrawerToggle} />
                    )}
                </ThemeProvider>
                <Routes>
                    <Route path="/" element={<AdminHomePage />} />
                    {adminConverterPages.map((page) => (
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
            </div>
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
