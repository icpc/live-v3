import { Legend } from "./types";
import styled from "styled-components";
import c from "../../../config";

const LegendsWrapper = styled.div`
    display: grid;
    grid-auto-flow: column;
    gap: ${c.STATISTICS_BAR_GAP};
    align-content: center;
    justify-content: end;

    flex-grow: 1;
    height: 100%;

    /* grid-template-columns: auto; */
`;

const LegendCardWrapper = styled.div`
    width: 100%;
    background-color: ${({ color }) => color};
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const LegendWrapper = styled.div`
    margin: ${c.LEGEND_VERTICAL_MARGIN} ${c.LEGEND_HORIZONTAL_MARGIN};

    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
    line-height: ${c.STATISTICS_BAR_HEIGHT};
    text-align: center;
`;

type LegendCardProps = { color: string; caption: string };

export const LegendCard = ({ color, caption }: LegendCardProps) => {
    return (
        <LegendCardWrapper color={color}>
            <LegendWrapper>{caption}</LegendWrapper>
        </LegendCardWrapper>
    );
};

type StatisticsLegendsProps = { legend: Legend };

export const StatisticsLegend = ({ legend }: StatisticsLegendsProps) => {
    return (
        <LegendsWrapper>
            {legend?.map((l) => (
                <LegendCard
                    key={l.caption}
                    caption={l.caption}
                    color={l.color}
                ></LegendCard>
            ))}
        </LegendsWrapper>
    );
};
