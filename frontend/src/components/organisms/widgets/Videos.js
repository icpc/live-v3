import React, { useState } from "react";
import styled, { keyframes } from "styled-components";
import PropTypes from "prop-types";
import { PICTURES_APPEAR_TIME, VERDICT_NOK } from "../../../config";
import { pushLog } from "../../../redux/debug";
import { useDispatch } from "react-redux";


const VideoContainerWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  justify-content: start;
  align-items: center;
`;


const VideoContainer = styled.div`
  display: grid;
  justify-content: center;
  text-align: center;
`;


const VideoCaptionWrap = styled.div`
  background-color: ${VERDICT_NOK};
  font-size: 24pt;
  font-family: Passageway, serif;
  color: white;
  align-self: stretch;
`;

const VideoImgWrap = styled.div`
  background-color: white;
`;

export const Video = ({ widgetData }) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const dispatch = useDispatch();
    return <VideoContainerWrap
        show={isLoaded}
    >
        <VideoContainer>
            <VideoWrap>
              <video width={100%} height={100%} src={widgetData.video.url}>
                  {widgetData.picture.name}
              </video>
            </VideoWrap>
            <VideoCaptionWrap>{widgetData.picture.name}</VideoCaptionWrap>
        </VideoContainer>
    </VideoContainerWrap>;
};

Pictures.propTypes = {
    widgetData: PropTypes.shape({
        picture: PropTypes.shape({
            url: PropTypes.string.isRequired,
            name: PropTypes.string.isRequired
        })
    }),
};

export default Pictures;
