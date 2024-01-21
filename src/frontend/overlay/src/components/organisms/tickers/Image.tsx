import React from "react";
import styled from "styled-components";
import { image } from "@shared/api";


export const ImageWrap = styled.div<{path: string}>`
    width: 100%;
    height: 100%;
    max-height: 100%;
    max-width: 100%;
    display: flex;
    padding: 0 16px;
    box-sizing: border-box;
    background-size: contain;
    background: url(${(props) => props.path}) no-repeat center;
`;

type ImageProps = {tickerSettings: image}

export const Image = ({ tickerSettings }: ImageProps) => {
    return <ImageWrap
        path={tickerSettings.path}
    />;
};

export default Image;

