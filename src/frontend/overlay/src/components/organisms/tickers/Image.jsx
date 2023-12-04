import React from "react";
import styled from "styled-components";


export const ImageWrap = styled.div`
  width: 100%;
  height: 100%;
  max-height: 100%;
  max-width: 100%;
  background: url(${(props) => props.path}) no-repeat;
  display: flex;
  padding: 0 16px;
  box-sizing: border-box;
  background-size: contain;
  background-position: center;
`;


export const Image = ({tickerSettings, part}) => {
    return <ImageWrap
        path={tickerSettings.path}
    />;
};

export default Image;

