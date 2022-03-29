import React, { useState } from "react";
import styled from "styled-components";
import PropTypes from "prop-types";

const VideosContainerWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: ${props => props.show ? "flex" : "none"};
  justify-content: start;
  align-items: center;
`;


const VideosContainer = styled.div`
  display: grid;
  justify-content: center;
  text-align: center;
`;

const VideosWrap = styled.div`
  background-color: white;
`;

export const Videos = ({ widgetData }) => {
    const [isLoaded] = useState(false);
    return <VideosContainerWrap
        show={isLoaded}
    >
        <VideosContainer>
            <VideosWrap>
                <video width={"100%"} height={"100%"} src={widgetData.videos.url}>
                    widgetData.videos.name
                </video>
            </VideosWrap>
        </VideosContainer>
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

export default Videos;
