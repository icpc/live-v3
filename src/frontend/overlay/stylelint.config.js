"use strict";

module.exports = {
    "extends": [
        "stylelint-config-recommended",
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
                "ignorePartialSupport": true
                // "ignore": ["multicolumn"]
            }
        ],
        // "order/properties-order": [
        //     undefined,
        //     {
        //         "severity": "warning",
        //     }
        // ],
        "block-no-empty": null,
        "no-empty-source": null
    },
    "customSyntax": "postcss-styled-syntax"
};
