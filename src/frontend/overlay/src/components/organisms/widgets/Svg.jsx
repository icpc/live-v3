import React from "react";
import c from "../../../config";
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

Svg.overrideTimeout = c.SVG_APPEAR_TIME;

export default Svg;
