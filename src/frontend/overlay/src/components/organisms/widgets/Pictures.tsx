import PropTypes from "prop-types";
import React, { useState } from "react";
import styled, { Keyframes, keyframes } from "styled-components";
import c from "../../../config";
import { pushLog } from "@/redux/debug";
import { useAppDispatch } from "@/redux/hooks";


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


const PicturesContainerWrap = styled.div<{
    show: boolean,
    animation: Keyframes,
    animationStyle: string
}>`
  position: relative;

  display: ${props => props.show ? "flex" : "none"};
  flex-direction: row;
  align-items: center;
  justify-content: center;

  width: 100%;
  height: 100%;

  animation: ${props => props.animation} ${c.PICTURES_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;


const PicturesContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;

  text-align: center;
  box-sizing: border-box;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  border: ${c.PICTURE_BORDER_SIZE} solid ${c.PICTURE_NAME_BACKGROUND_COLOR};
  overflow: hidden;
  max-width: 100%;
  max-height: 100%;
  background-color: ${c.PICTURE_NAME_BACKGROUND_COLOR};
`;


const PicturesCaptionWrap = styled.div`
  font-family: ${c.PICTURE_NAME_FONT_FAMILY}, serif;
  font-size: ${c.PICTURE_NAME_FONT_SIZE};
  color: ${c.PICTURE_NAME_FONT_COLOR};
`;

const PicturesImg = styled.img`
  flex-grow: 1;
  flex-shrink: 1;
  min-width: 0;
  min-height: 0;
  object-fit: contain;
`;

export const Pictures = ({ widgetData, transitionState }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useAppDispatch();
    return <PicturesContainerWrap
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        <PicturesContainer>
            <PicturesImg
                src={widgetData.picture.url}
                alt={widgetData.picture.name}
                onLoad={() => setIsLoaded(true)}
                onError={() => {setIsLoaded(false); dispatch(pushLog("ERROR on loading image in Picture widget"));}}
            />
            <PicturesCaptionWrap>{widgetData.picture.name}</PicturesCaptionWrap>
        </PicturesContainer>
    </PicturesContainerWrap>;
};

Pictures.propTypes = {
    widgetData: PropTypes.shape({
        picture: PropTypes.shape({
            url: PropTypes.string.isRequired,
            name: PropTypes.string.isRequired
        })
    }),
    transitionState: PropTypes.string
};

Pictures.ignoreAnimation = true;
Pictures.overrideTimeout = c.PICTURES_APPEAR_TIME;

export default Pictures;
