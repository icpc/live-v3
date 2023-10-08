import { StackedBarsData } from "./types";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";

const BarsWrapper = styled.div`
  width: 100%;
  height: 100%;
  display: grid;
  gap: ${c.STATISTICS_BAR_GAP};
  grid-auto-flow: column;
  grid-template-rows: repeat(${({rowsCount}) => rowsCount}, 1fr);
`

const BarWrapper = styled.div`
  width: 100%;
  height: ${c.STATISTICS_BAR_HEIGHT};
  line-height: ${c.STATISTICS_BAR_HEIGHT};
  display: grid;
  gap: 0;
  grid-template-columns: ${c.STATISTICS_BAR_HEIGHT} auto;
`

const BarName = styled(ProblemLabel)`
  width: ${c.STATISTICS_BAR_HEIGHT};
  background-color: ${({color}) => color};
  font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  text-align: center;
`
//todo: set font widget

const BarValues = styled.div`
  width: 100%;
  display: flex;
  justify-content: flex-start;
  background-color: ${c.QUEUE_ROW_BACKGROUND};
  border-radius: 0 ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS} 0;
  overflow: hidden;
`

const BarValue = styled.div.attrs(({ value, caption }) => ({
    style: {
        width: `calc(max(${value * 100}%, ${value === 0 ? 0 : ((caption?.length ?? -1) + 1)}ch))`,
    }
}))`
  height: ${c.STATISTICS_BAR_HEIGHT};
  line-height: ${c.STATISTICS_BAR_HEIGHT};
  transition: width linear ${c.STATISTICS_CELL_MORPH_TIME}ms;
  overflow: hidden;
  box-sizing: border-box;

  font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};

  background-color: ${({color}) => color};
  color: ${c.STATISTICS_TITLE_COLOR};
  text-align: center;
`

type StackedBarsStatisticsProps = { data: StackedBarsData, rowsCount: number };

export const StackedBarsStatistics = ({data, rowsCount}: StackedBarsStatisticsProps) => {
    return (
        <BarsWrapper rowsCount={rowsCount}>
            {data.bars.map((b) => {
                return (
                    <BarWrapper key={b.name}>
                        <BarName problemColor={b.color} letter={b.name}/>
                        <BarValues>
                            {b.values.map(v => (
                                <BarValue key={v.color} color={v.color} value={v.value} caption={v.caption}>
                                    {v.caption}
                                </BarValue>
                            ))}
                        </BarValues>
                    </BarWrapper>
                )
            })}
        </BarsWrapper>
    );
}
