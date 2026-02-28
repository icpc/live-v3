import { useEffect, useState } from "react";
import AppNav from "./AppNav.tsx";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { SnackbarProvider } from "notistack";
import { createApiGet } from "@shared/utils";
import {
    setFavicon,
    isShouldUseDarkColor,
} from "./utils";
import { createTheme, ThemeProvider } from "@mui/material";
import { BACKEND_ROOT } from "./config";
import { faviconTemplate } from "./styles.ts";
import {
    ReloadHandleContext,
    useReloadHandleService,
} from "@admin-contest-info/services/reloadHandler.ts";
import ContestLog from "./components/pages/ContestInfo";
import ConfigurationsEditor from "./components/ConfigurationEditor.tsx";
import { AdminLayout } from "admin-router";

const getTheme = (contestColor) => {
    if (!contestColor) {
        return createTheme({
            palette: {
                text: {
                    primary: "#FFFFFF",
                },
            },
        });
    }
    const textColor = isShouldUseDarkColor(contestColor)
        ? "#000000"
        : "#FFFFFF";
    return createTheme({
        palette: {
            mode: "light",
            primary: {
                main: contestColor,
                contrastText: textColor,
            },
            text: {
                primary: textColor,
            },
        },
    });
};

export function AdminApp() {
    const [contestColor, setContestColor] = useState(null);

    useEffect(() => {
        createApiGet(BACKEND_ROOT)("/api/overlay/visualConfig.json").then(
            (c) => {
                if (c["CONTEST_COLOR"]) {
                    setContestColor(c["CONTEST_COLOR"]);
                    setFavicon(
                        faviconTemplate
                            .replaceAll("{CONTEST_COLOR}", c["CONTEST_COLOR"])
                            .replaceAll(
                                "{TEXT_COLOR}",
                                isShouldUseDarkColor(contestColor)
                                    ? "#000000"
                                    : "#FFFFFF",
                            ),
                    );
                }
                if (c["CONTEST_CAPTION"]) {
                    document.title =
                        c["CONTEST_CAPTION"] + " — ICPC Live 3 Admin";
                }
            },
        );
    }, []);

    const reloadHandleService = useReloadHandleService();

    return (
        <AdminLayout>
            <ReloadHandleContext.Provider value={reloadHandleService}>
                <SnackbarProvider maxSnack={5}>
                    <div className="App">
                        <ThemeProvider theme={getTheme(contestColor)}>
                            <AppNav />
                        </ThemeProvider>
                        <Routes>
                            <Route path="/" element={<Navigate to="/contestInfo" />} />
                            <Route
                                path="/contestInfo"
                                element={<ContestLog />}
                            />
                            <Route
                                path="/configurationsEditor"
                                element={<ConfigurationsEditor />}
                            />
                        </Routes>
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
