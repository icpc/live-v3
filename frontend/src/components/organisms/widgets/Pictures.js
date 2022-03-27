import React from "react";
import styled, { keyframes } from "styled-components";
import { WIDGET_TRANSITION_TIME } from "../../../config";


const slideIn = keyframes`
  from {
    left: 100%;
  }
  to {
    left: 0;
  }
`;

const slideOut = keyframes`
 from {
   left: 0;
 }
  to {
    left: 100%;
  }
`;

export const transition = {
    entering: { animationInner: slideIn },
    entered:  {  },
    exiting:  { animationInner: slideOut },
    exited:  { },
};

const PicturesContainerWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: ${props => props.animation} 3000ms linear;
`;


const PicturesContainer = styled.div`
  display: grid;
  justify-content: center;
  text-align: center;
`;


const PicturesCaptionWrap = styled.div`
  background-color: rgb(136, 31, 27);
  font-size: 24pt;
  font-family: Passageway, serif;
  color: white;
  align-self: stretch;
`;

const PicturesImgWrap = styled.div`
  background-color: white;
`;

export const Pictures = ({ widgetData, animationInner }) => {
    return <PicturesContainerWrap animation={animationInner}><PicturesContainer>
        <PicturesImgWrap><img  src={widgetData.picture.url} alt = {widgetData.picture.name}/> </PicturesImgWrap>
        <PicturesCaptionWrap>{widgetData.picture.name}</PicturesCaptionWrap>
    </PicturesContainer></PicturesContainerWrap>;
};
export default Pictures;
