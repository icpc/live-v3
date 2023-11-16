module.exports = {
    "ignorePatterns": [
        "build/**"
    ],
    "parser": "@typescript-eslint/parser",
    "env": {
        "node": true,
        "browser": true,
        "es2021": true
    },
    "extends": [
        "eslint:recommended",
        "plugin:react/recommended",
        "plugin:@typescript-eslint/recommended"
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
        "@typescript-eslint",
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
        "no-unused-vars": [
            "warn"
        ],
        "react/prop-types": [
            "off"
        ],
        // suppress errors for missing 'import React' in files
        "react/react-in-jsx-scope": "off",
        // allow jsx syntax in js files (for next.js project)
        "react/jsx-filename-extension": [1, { "extensions": [".jsx", ".tsx"] }], //should add ".ts" if typescript project
    },
    root: true
};
