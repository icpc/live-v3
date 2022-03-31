import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import PropTypes from "prop-types";
import { PICTURES_APPEAR_TIME } from "../../../config";
import { pushLog } from "../../../redux/debug";
import { useDispatch } from "react-redux";


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
  animation: ${props => props.animation} ${PICTURES_APPEAR_TIME}ms ${props => props.animationStyle};
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

export const Pictures = ({ widgetData, transitionState }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useDispatch();
    return <PicturesContainerWrap
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        <PicturesContainer>
            <PicturesImgWrap>
                <img
                    src={widgetData.picture.url}
                    alt={widgetData.picture.name}
                    onLoad={() => setIsLoaded(true)}
                    onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
                />
            </PicturesImgWrap>
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
Pictures.overrideTimeout = PICTURES_APPEAR_TIME;

export default Pictures;
