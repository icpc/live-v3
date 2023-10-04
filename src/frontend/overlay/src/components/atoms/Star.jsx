import React from "react";
import styled from "styled-components";
import star from "../../assets/icons/star.svg";
import c from "../../config";


const StarIconWrap = styled.img`
    position: absolute;
    top: 0;
    right: 0;
    width: ${c.STAR_SIZE}px;
    height: ${c.STAR_SIZE}px;
`;
export const StarIcon = () => {
    return <StarIconWrap src={star} alt="first"/>;
};
