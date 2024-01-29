import PropTypes from "prop-types";
import React, { useState } from "react";
import { useDispatch } from "react-redux";
import styled, { keyframes } from "styled-components";
import c from "../../../config";
import { pushLog } from "../../../redux/debug";


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


const PicturesContainerWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  justify-content: start;
  align-items: center;
  flex-direction: row;
  animation: ${props => props.animation} ${c.PICTURES_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;


const PicturesContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  justify-content: center;
  text-align: center;
`;


const PicturesCaptionWrap = styled.div`
  background-color: ${c.CONTEST_COLOR};
  font-size: 24pt;
  //font-family: Passageway, serif;
  color: white;
  align-self: stretch;
`;

const PicturesImg = styled.img`
  object-fit: contain;
  flex-shrink: 1;
  flex-grow: 1;
  max-width: 100%;
`;

export const Pictures = ({ widgetData, transitionState }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useDispatch();
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
                onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
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
