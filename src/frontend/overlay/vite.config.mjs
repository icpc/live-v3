import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tsconfigPaths from "vite-tsconfig-paths";

// https://vitejs.dev/config/

export default defineConfig({
    plugins: [
        tsconfigPaths(),
        react({
            babel: {
                plugins: [
                    ["babel-plugin-react-compiler", {
                        target: "19"
                    }]
                ]
            }
        }),
   ],
    base: process.env.PUBLIC_URL ?? "/",
    build: {
        target: "esnext",
        outDir: process.env.BUILD_PATH ?? "dist"
    },
    experimental: {
        enableNativePlugin: true,
    }
});
