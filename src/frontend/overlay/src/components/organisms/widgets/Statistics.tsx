import styled from "styled-components";
import { useSelector } from "react-redux";
import c from "../../../config";
import { stackedBarsData } from "../../../statistics/barData";
import { StatisticsLegend } from "../../molecules/statistics/StatisticsLegend";
import { StackedBars } from "../../molecules/statistics/StackedBars";
import { useElementSize } from "usehooks-ts";

const StatisticsWrap = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
  background-color: ${c.CONTEST_COLOR};
  background-repeat: no-repeat;
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
  padding: 8px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const Header = styled.div`
  font-size: 32px;
  line-height: 44px;
  color: white;
  width: 100%;
  gap: 16px;
  display: flex;
`;
const Title = styled.div`
  font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
    display: flex;
    align-items: center;
`;

const Caption = styled.div``;


export const Statistics = () => {
    // @ts-ignore-start
    const resultType = useSelector(state => state.contestInfo?.info?.resultType);
    // @ts-ignore
    const statistics = useSelector(state => state.statistics.statistics);
    // @ts-ignore
    const count = useSelector(state => state.contestInfo?.info?.teams?.length);
    // @ts-ignore
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const data = stackedBarsData(resultType, tasks, statistics, count);

    const [componentRef, { height }] = useElementSize();
    const [headerRef, { height: headerHeight }] = useElementSize();

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
