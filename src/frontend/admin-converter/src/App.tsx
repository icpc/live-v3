import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AdminLayout } from "admin-router";

export function AdminApp() {
    return (
        <AdminLayout>
            <div className="App">
                <Routes>
                    <Route path="/" element={<div>Converter Admin Stub</div>} />
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
