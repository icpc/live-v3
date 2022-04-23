import React from "react";
import styled from "styled-components";
import { SVG_APPEAR_TIME } from "../../../config";

const SvgContainer = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;


export const Svg = ({ widgetData }) => {
    return <SvgContainer>
        <img
            src={widgetData.content}
            alt="None"
        />
    </SvgContainer>;
};

Svg.overrideTimeout = SVG_APPEAR_TIME;

export default Svg;
