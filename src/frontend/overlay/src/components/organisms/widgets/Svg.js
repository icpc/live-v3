import React from "react";
import { SVG_APPEAR_TIME } from "../../../config";
import PropTypes from "prop-types";

export const Svg = ({ widgetData }) => {
    return <object
        type="image/svg+xml"
        data={widgetData.content}
    />;
};

Svg.propTypes = {
    widgetData: PropTypes.shape({
        content: PropTypes.string.isRequired
    })
};

Svg.overrideTimeout = SVG_APPEAR_TIME;
Svg.ignoreAnimation = true;

export default Svg;
