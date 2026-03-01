import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AdminLayout } from "admin-router";
import AppNav from "./AppNav";
import { ClicsAdmin } from "./components/pages/ClicsAdmin";
import { PCMSAdmin } from "./components/pages/PCMSAdmin";
import { IcpcAdmin } from "./components/pages/IcpcAdmin";
import { ReactionsAdmin } from "./components/pages/ReactionsAdmin";
import { LoginAdmin } from "./components/pages/LoginAdmin";

export function AdminApp() {
    return (
        <AdminLayout>
            <AppNav />
            <div className="App">
                <Routes>
                    <Route path="/" element={<Navigate to="/clics" replace />} />
                    <Route path="/clics" element={<ClicsAdmin />} />
                    <Route path="/pcms" element={<PCMSAdmin />} />
                    <Route path="/icpc" element={<IcpcAdmin />} />
                    <Route path="/reactions" element={<ReactionsAdmin />} />
                    <Route path="/session" element={<LoginAdmin />} />
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
