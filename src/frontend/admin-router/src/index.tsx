import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { BrowserRouter } from "react-router-dom";

const container = document.getElementById("root");
const root = createRoot(container!);
root.render(
    <React.StrictMode>
        <BrowserRouter basename={import.meta.env.BASE_URL ?? ""}>
            <App />
        </BrowserRouter>
    </React.StrictMode>
);
