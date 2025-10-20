import React from "react";
import styled from "styled-components";
import c from "../../../config";

const AdvertisementContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;

  width: 100%;
  height: 100%;
`;

// TODO: move this to constants.js
const AdvertisementWrap = styled.div`
  padding: ${c.ADVERTISEMENT_VERTICAL_PADDING} ${c.ADVERTISEMENT_HORIZONTAL_PADDING};

  font-family: ${c.ADVERTISEMENT_FONT_FAMILY}, serif;
  font-size: ${c.ADVERTISEMENT_FONT_SIZE};
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  color: ${c.ADVERTISEMENT_COLOR};

  background-color: ${c.ADVERTISEMENT_BACKGROUND};
  border-radius: ${c.ADVERTISEMENT_BORDER_RADIUS};
  text-align: center;
`;


export const Advertisement = ({ widgetData }) => {
    return <AdvertisementContainer>
        <AdvertisementWrap>{widgetData.advertisement.text}</AdvertisementWrap>
    </AdvertisementContainer>;
};
export default Advertisement;
