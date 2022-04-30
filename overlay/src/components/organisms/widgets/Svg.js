import React from "react";
import { SVG_APPEAR_TIME } from "../../../config";
import PropTypes from "prop-types";

export const Svg = ({ widgetData }) => {
    return <img
        src={widgetData.content}
        alt="None"
    />;
};

Svg.propTypes = {
    widgetData: PropTypes.shape({
        content: PropTypes.string.isRequired
    })
};

Svg.overrideTimeout = SVG_APPEAR_TIME;

export default Svg;
