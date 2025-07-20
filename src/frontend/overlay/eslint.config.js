const js = require("@eslint/js");
const globals = require("globals");
const react = require("eslint-plugin-react");
const reactHooks = require("eslint-plugin-react-hooks");
const tseslint = require("typescript-eslint");

module.exports = tseslint.config(
    { ignores: ["dist/**/*", "build/**/*", "node_modules/**/*", "eslint.config.js"] }, // Add eslint.config.js here
    
    js.configs.recommended,
    
    // React config for ALL files (JS/JSX/TS/TSX)
    {
        files: ["**/*.{js,jsx,ts,tsx}"],
        languageOptions: {
            ecmaVersion: 12,
            sourceType: "module",
            globals: {
                ...globals.node,
                ...globals.browser,
            },
            parserOptions: {
                ecmaFeatures: {
                    jsx: true,
                },
            },
        },
        
        plugins: {
            react,
            "react-hooks": reactHooks,
        },
        
        settings: {
            react: {
                version: "detect",
            },
        },
        
        rules: {
            ...react.configs.recommended.rules,
            ...reactHooks.configs.recommended.rules,
            
            "indent": ["error", 4],
            "linebreak-style": ["warn", "unix"],
            "quotes": ["warn", "double"],
            "semi": ["warn", "always"],
            "eol-last": ["warn", "always"],
            "no-case-declarations": "off",
            "object-curly-spacing": ["warn", "always"],
            "react/prop-types": ["off"],
            "react/display-name": ["off"],
            "react/react-in-jsx-scope": "off",
            "react/jsx-filename-extension": [1, {
                "extensions": [".jsx", ".tsx"],
            }],
            "no-restricted-imports": ["error", {
                "paths": [{
                    "name": "react-redux",
                    "importNames": ["useDispatch", "useSelector"],
                    "message": "You should use useAppDispatch and useAppSelector from \"@/redux/hooks\";",
                }],
            }],
        },
    },
    
    // TypeScript config
    ...tseslint.configs.recommended,
    {
        files: ["**/*.{ts,tsx}"],
        languageOptions: {
            parserOptions: {
                project: true,
                tsconfigRootDir: __dirname,
            },
        },
        rules: {
            "@typescript-eslint/switch-exhaustiveness-check": "off",
            "@typescript-eslint/no-unused-vars": ["warn", {
                "argsIgnorePattern": "^_",
            }],
            "@typescript-eslint/no-this-alias": "off",
            "@typescript-eslint/no-require-imports": "off", // Allow require in config files
        },
    }
);