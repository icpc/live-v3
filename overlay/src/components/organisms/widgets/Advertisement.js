import React from "react";
import styled from "styled-components";

const AdvertisementContainer = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const AdvertisementWrap = styled.div` // TODO: move this to constants.js
  padding: 8px;
  background-color: white;
  border-radius: 12px;
  font-size: 24pt;
  font-weight: 700;
  font-family: Urbanist, Passageway, serif;
  color: black;
`;


export const Advertisement = ({ widgetData }) => {
    return <AdvertisementContainer>
        <AdvertisementWrap>{widgetData.advertisement.text}</AdvertisementWrap>
    </AdvertisementContainer>;
};
export default Advertisement;
