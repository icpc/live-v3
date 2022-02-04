import React from "react";
import styled from "styled-components";

const AdvertisementContainer = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const AdvertisementWrap = styled.div`
  padding: 8px;
  background-color: rgb(136, 31, 27);
  font-size: 24pt;
  font-family: Passageway, serif;
  color: white
`;


export const Advertisement = ({ widgetData }) => {
    return <AdvertisementContainer>
        <AdvertisementWrap>{widgetData.advertisement.text}</AdvertisementWrap>
    </AdvertisementContainer>;
};
export default Advertisement;
