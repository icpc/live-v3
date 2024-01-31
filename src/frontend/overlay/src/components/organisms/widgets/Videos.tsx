import React, { useState } from "react";
import styled, { Keyframes, keyframes } from "styled-components";
import PropTypes from "prop-types";
import { pushLog } from "@/redux/debug";
import c from "../../../config";
import { useAppDispatch } from "@/redux/hooks";

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


const VideosContainerWrap = styled.div<{show: boolean, animation: Keyframes, animationStyle: string}>`
  position: absolute;

  display: ${props => props.show ? "flex" : "none"};
  align-items: center;
  justify-content: start;

  width: 100%;
  height: 100%;

  animation: ${props => props.animation} ${c.VIDEO_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;


const VideosContainer = styled.video`
  display: grid;
  justify-content: center;

  width: 100%;
  height: 100%;

  text-align: center;
`;

export const Videos = ({ widgetData, transitionState }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useAppDispatch();

    return <VideosContainerWrap
        show={isLoaded}
        animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
        animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
    >
        <VideosContainer
            src={widgetData.video.url}
            onCanPlay={() => setIsLoaded(true)}
            onError={() => {setIsLoaded(false); dispatch(pushLog("ERROR on loading image in Picture widget"));}}
            autoPlay
            muted/>
    </VideosContainerWrap>;
};

Videos.propTypes = {
    widgetData: PropTypes.shape({
        video: PropTypes.shape({
            url: PropTypes.string.isRequired,
            name: PropTypes.string.isRequired
        })
    }),
};

Videos.ignoreAnimation = true;
Videos.overrideTimeout = c.VIDEO_APPEAR_TIME;

export default Videos;
