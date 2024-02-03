"use strict";

module.exports = {
    "extends": [
        "stylelint-config-recommended",
        "stylelint-config-styled-components",
        // "stylelint-config-clean-order/warning" // Disabled for now...
    ],
    "plugins": [
        "stylelint-no-unsupported-browser-features"
    ],
    "rules": {
        "plugin/no-unsupported-browser-features": [
            true,
            {
                "severity": "error",
                "ignore": ["multicolumn"]
            }
        ],
        // "order/properties-order": [
        //     undefined,
        //     {
        //         "severity": "warning",
        //     }
        // ],
        "block-no-empty": null
    },
    "customSyntax": "postcss-styled-syntax"
};
