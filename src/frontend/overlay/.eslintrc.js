module.exports = {
    "ignorePatterns": [
        "dist/**",
        "build/**",
        "node_modules/**"
    ],
    // "parser": "@typescript-eslint/parser",
    "env": {
        "node": true,
        "browser": true,
        "es2021": true
    },
    "extends": [
        "eslint:recommended",
        "plugin:react/recommended",
        // "plugin:@typescript-eslint/eslint-recommended",
        // "plugin:@typescript-eslint/recommended"
    ],
    "settings": {
        "react": {
            "version": "detect"
        }
    },
    "parserOptions": {
        "ecmaFeatures": {
            "jsx": true
        },
        "ecmaVersion": 12,
        "sourceType": "module"
    },
    "plugins": [
        // "@typescript-eslint",
        "react",
        "react-hooks"
    ],
    "rules": {
        "indent": [
            "error",
            4
        ],
        "linebreak-style": [
            "warn",
            "unix"
        ],
        "quotes": [
            "warn",
            "double"
        ],
        "semi": [
            "warn",
            "always"
        ],
        "eol-last": [
            "warn",
            "always"
        ],
        "no-case-declarations": "off",
        "object-curly-spacing": [
            "warn",
            "always"
        ],
        "@typescript-eslint/no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
        // "no-unused-vars": [
        //     "warn"
        // ],
        "react/prop-types": [
            "off"
        ],
        "react/display-name": [
            "off"
        ],
        // suppress errors for missing 'import React' in files
        "react/react-in-jsx-scope": "off",
        // allow jsx syntax in js files (for next.js project)
        "react/jsx-filename-extension": [1, { "extensions": [".jsx", ".tsx"] }], //should add ".ts" if typescript project

        "no-restricted-imports": ["error", {
            "paths": [{
                "name": "react-redux",
                "importNames": ["useDispatch", "useSelector"],
                "message": "You should use useAppDispatch and useAppSelector from \"@/redux/hooks\";"
            }]
        }]
    },
    "overrides": [
        {
            "files": ["*.{ts,tsx}"],
            "rules": {
                "@typescript-eslint/switch-exhaustiveness-check": "error",
            },
            "parser": "@typescript-eslint/parser",
            "plugins": [
                "@typescript-eslint"
            ],
            "extends": [
                "plugin:@typescript-eslint/recommended"
            ],
            "parserOptions": {
                "project": "./tsconfig.json"
            }
        }
    ],
    root: true
};
