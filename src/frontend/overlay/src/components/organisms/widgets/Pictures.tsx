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
  flex-wrap: wrap;
  justify-content: center;

  text-align: center;
  box-sizing: border-box;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  border: 5px solid ${c.PICTURE_NAME_BACKGROUND_COLOR};
  overflow: hidden;
  background-color: ${c.PICTURE_NAME_BACKGROUND_COLOR};
`;


const PicturesCaptionWrap = styled.div`
  align-self: stretch;

  font-family: Passageway, serif;
  font-size: 24pt;
  color: ${c.PICTURE_NAME_FONT_COLOR};
`;

const PicturesImg = styled.img`
  flex-grow: 1;
  flex-shrink: 1;
  max-width: 100%;
  max-height: 100%;
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
