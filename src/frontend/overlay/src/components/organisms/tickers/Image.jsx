import {memo, React, useCallback, useEffect, useRef, useState} from "react";
import styled from "styled-components";
import c from "../../../config";
import {TextWrap} from "./Text";
import {getTextWidth} from "../../atoms/ShrinkingBox";
 
 
export const ImageWrap = styled.div`
    background: url(${(props) => props.path});
    width: 153px;
    height: 30px;
    margin: 0 -16px;
`;
 

export const Image = ({tickerSettings, part}) => {
    return <TextWrap part={part}>
        <ImageWrap
            path={"/media/" + tickerSettings.path}
        />
    </TextWrap>;
};

export default Image;

