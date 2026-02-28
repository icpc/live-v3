const js = require("@eslint/js");
const globals = require("globals");
const reactRefresh = require("eslint-plugin-react-refresh");
const reactCompiler = require("eslint-plugin-react-compiler");
const tseslint = require("typescript-eslint");

module.exports = tseslint.config(
    { ignores: ["dist/**/*", "build/**/*", "node_modules/**/*"] }, // Add eslint.config.js here

    js.configs.recommended,
    ...tseslint.configs.recommended,

    {
        files: ["**/*.{ts,tsx}"],
        languageOptions: {
            ecmaVersion: 2020,
            globals: globals.browser,
        },
        plugins: {
            "react-refresh": reactRefresh,
            "react-compiler": reactCompiler,
        },
        rules: {
            "react-refresh/only-export-components": [
                "warn",
                {
                    allowConstantExport: true,
                },
            ],
            "react-compiler/react-compiler": "warn",
        },
    },
);
