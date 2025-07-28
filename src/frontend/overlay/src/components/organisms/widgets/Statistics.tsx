import styled from "styled-components";
import c from "../../../config";
import { stackedBarsData } from "../../../statistics/barData";
import { StatisticsLegend } from "../../molecules/statistics/StatisticsLegend";
import { StackedBars } from "../../molecules/statistics/StackedBars";
import { useResizeObserver } from "usehooks-ts";
import { useAppSelector } from "@/redux/hooks";
import { useRef } from "react";

const StatisticsWrap = styled.div`
  position: relative;

  display: flex;
  flex-direction: column;
  gap: 8px;

  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 8px 16px;

  background-color: ${c.CONTEST_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const Header = styled.div`
  display: flex;
  flex-flow: row;
  gap: 16px;

  width: 100%;

  font-size: 32px;
  line-height: 44px;
  color: white;
`;
const Title = styled.div`
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
`;

const Caption = styled.div`
    flex-grow: 1;
`;


export const Statistics = () => {
    const statistics = useAppSelector((state) => state.statistics.statistics);
    const tasks = useAppSelector(state => state.contestInfo?.info?.problems);
    const data = stackedBarsData(tasks, statistics);

    const componentRef = useRef<HTMLDivElement>(null);
    const headerRef = useRef<HTMLDivElement>(null);
    
    const { height = 0 } = useResizeObserver({ ref: componentRef });
    const { height: headerHeight = 0 } = useResizeObserver({ ref: headerRef });

    return (
        <StatisticsWrap ref={componentRef}>
            <Header ref={headerRef}>
                <Title>{c.STATISTICS_TITLE}</Title>
                <Caption>{c.STATISTICS_CAPTION}</Caption>
                <StatisticsLegend legend={data.legend}></StatisticsLegend>
            </Header>

            {data.data && data.data.length > 0 && <StackedBars data={data.data} height={height - headerHeight - 8} />}
        </StatisticsWrap>
    );
};
export default Statistics;
