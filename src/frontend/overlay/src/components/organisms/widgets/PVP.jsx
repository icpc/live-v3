import React, { useLayoutEffect } from "react";
import styled from "styled-components";
import c from "../../../config";
import {
    ContestantViewHolder
} from "../holder/ContestantViewHolder";
import {ContestantViewLine} from "../../molecules/info/ContestantViewLine";


const PVPViewHolder = styled(ContestantViewHolder)`
  position: absolute;
  padding: 0;
  top: ${props => props.top};
  bottom: ${props => props.bottom};
  left: ${props => props.left};
  right: ${props => props.right};
  height: ${props => props.height};
  width: ${props => props.width};
`

export const PVP = ({ mediaContent, settings, setLoadedComponents, location }) => {
    return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
        const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
        const [positionLeft, positionRight] = index === 0 ? ["0", "auto"] : ["auto", "0"]
        const height = index === 0 ? "50%" : "auto"
        const width = index === 0 ? location.sizeY / 2 * 16 / 9 + "px" : location.sizeX - location.sizeY / 2 * 16 / 9 + "px"

        const top = c.QUEUE_ROW_HEIGHT2;
        console.log(top);

        const [positionTop, positionBottom] =
            settings.position === "PVP_TOP" ? (index === 0 ? ["auto", "50%"] : ["auto", "50%"])
               :  (index === 0 ? ["50%", "auto"] : ["50%", "auto"])

        useLayoutEffect(() => onLoadStatus(true), []);

        if (c.isMedia) {
            return <PVPViewHolder key={index} onLoadStatus={onLoadStatus} media={c} top={positionTop} bottom={positionBottom}
                        left={positionLeft} right={positionRight} height={height} width={width}/>;
        } else {
            return <ContestantViewLine key={index} onLoadStatus={onLoadStatus} teamId={c.teamId} isTop={settings.position === "PVP_TOP"}/>;
        }
    });
};
PVP.ignoreAnimation = true;
PVP.overrideTimeout = c.PVP_APPEAR_TIME;
export default PVP;
