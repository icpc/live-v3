import {Legend} from "./types";
import styled from "styled-components";
import c from "../../../config";


const LegendsWrapper = styled.div`
  width: 100%;
  height: 100%;
  display: grid;
  gap: ${c.STATISTICS_BAR_GAP};
  //grid-template-columns: auto;
  grid-auto-flow: column;
  justify-content: end;
  align-content: center;
`

const LegendCardWrapper = styled.div`
  width: 100%;
  background-color: ${({color}) => color};
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`

const LegendWrapper = styled.div`
  line-height: ${c.STATISTICS_BAR_HEIGHT};
  font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};  
  text-align: center;
  margin: 8px 16px;
`

type LegendCardProps = { color: string; caption: string };

export const LegendCard = ({ color, caption }: LegendCardProps) => {
    return (
        <LegendCardWrapper color={color}>
            <LegendWrapper>
                {caption}
            </LegendWrapper>
        </LegendCardWrapper>
    );
}

type StatisticsLegendsProps = { legend: Legend };

export const StatisticsLegend = ({legend}: StatisticsLegendsProps) => {
    return (
        <LegendsWrapper>
            {legend?.map((l) => (
                <LegendCard key={l.caption} caption={l.caption} color={l.color}></LegendCard>
            ))}
        </LegendsWrapper>
    );
}
