const js = require("@eslint/js");
const globals = require("globals");
const reactRefresh = require("eslint-plugin-react-refresh");
const tseslint = require("typescript-eslint");

module.exports = tseslint.config(
  { ignores: ["**/dist", "**/.eslintrc.cjs"] },
  
  js.configs.recommended,
  ...tseslint.configs.recommended,
  
  {
    files: ["**/*.{js,jsx,ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      "react-refresh": reactRefresh,
    },
    rules: {
      "react-refresh/only-export-components": ["warn", {
        allowConstantExport: true,
      }],
    },
  }
);