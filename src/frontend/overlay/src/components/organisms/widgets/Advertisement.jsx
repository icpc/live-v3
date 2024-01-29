import React from "react";
import styled from "styled-components";
import c from "../../../config";

const AdvertisementContainer = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

// TODO: move this to constants.js
const AdvertisementWrap = styled.div`
  padding: 10px 20px;
  background-color: ${c.CONTEST_COLOR};
  //border-radius: 12px;
  font-size: 24pt;
  // font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
  font-family: TTFors, sans-serif;
    font-weight: bold;
  color: white;
`;


export const Advertisement = ({ widgetData }) => {
    return <AdvertisementContainer>
        <AdvertisementWrap>{widgetData.advertisement.text}</AdvertisementWrap>
    </AdvertisementContainer>;
};
export default Advertisement;
