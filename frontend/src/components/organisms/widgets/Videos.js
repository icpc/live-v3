import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import PropTypes from "prop-types";
import { useDispatch } from "react-redux";
import { pushLog } from "../../../redux/debug";
import { VIDEO_APPEAR_TIME } from "../../../config";

const slideIn = keyframes`
  from {
    opacity: 0.1;
  }
  to {
     opacity: 1;
  }
`;

const slideOut = keyframes`
  from {
     opacity: 1;
  }
  to {
      opacity: 0.1;
  }
`;


const VideosContainerWrap = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  justify-content: start;
  align-items: center;
  animation: ${props => props.animation} ${VIDEO_APPEAR_TIME}ms ${props => props.animationStyle};
`;


const VideosContainer = styled.video`
  display: grid;
  justify-content: center;
  text-align: center;
  width: 100%;
  height: 100%;
`;

export const Videos = ({ widgetData, transitionState }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useDispatch();

    return <VideosContainerWrap
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        <VideosContainer
            src={"https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"}
            onCanPlay={() => setIsLoaded(true)}
            onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
            autoPlay
            muted/>
    </VideosContainerWrap>;
};

Videos.propTypes = {
    widgetData: PropTypes.shape({
        picture: PropTypes.shape({
            url: PropTypes.string.isRequired,
            name: PropTypes.string.isRequired
        })
    }),
};

Videos.ignoreAnimation = true;
Videos.overrideTimeout = VIDEO_APPEAR_TIME;

export default Videos;
