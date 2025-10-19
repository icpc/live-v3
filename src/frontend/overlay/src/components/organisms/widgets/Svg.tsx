import React from "react";
import c from "../../../config";

interface SvgProps {
    widgetData: {
        content: string;
    };
}

export const Svg: React.FC<SvgProps> = ({ widgetData }) => {
    return <object
        type="image/svg+xml"
        data={widgetData.content}
    />;
};


Svg.overrideTimeout = c.SVG_APPEAR_TIME;

export default Svg;
