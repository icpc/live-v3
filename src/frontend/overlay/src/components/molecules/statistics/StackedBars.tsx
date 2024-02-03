import { useMemo } from "react";
import { StackedBarsData } from "./types";
import styled from "styled-components";
import c from "../../../config";
import { ProblemLabel } from "../../atoms/ProblemLabel";

const BarsWrapper = styled.div<{rowsCount: number}>`
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(${({ rowsCount }) => rowsCount}, 1fr);
  gap: ${c.STATISTICS_BAR_GAP};

  width: 100%;
  height: 100%;
`;

const BarWrapper = styled.div`
  display: grid;
  grid-template-columns: ${c.STATISTICS_BAR_HEIGHT} auto;
  gap: 0;

  width: 100%;
  height: ${c.STATISTICS_BAR_HEIGHT};

  line-height: ${c.STATISTICS_BAR_HEIGHT};
`;

const BarName = styled(ProblemLabel)`
  width: ${c.STATISTICS_BAR_HEIGHT};

  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
  text-align: center;

  background-color: ${({ color }) => color};
`;
//todo: set font widget

const BarValues = styled.div`
  overflow: hidden;
  display: flex;
  justify-content: flex-start;

  width: 100%;

  background-color: ${c.QUEUE_ROW_BACKGROUND};
  border-radius: 0 ${c.GLOBAL_BORDER_RADIUS} ${c.GLOBAL_BORDER_RADIUS} 0;
`;

type BarValueProps = { value: number, caption: string }; // Note: this is used twice for BarValue. Look closely

const BarValue = styled.div.attrs<BarValueProps>(({ value, caption }) => ({
    style: {
        width: `calc(max(${value * 100}%, ${value === 0 ? 0 : ((caption?.length ?? -1) + 1)}ch))`,
    }
}))<BarValueProps>`
  overflow: hidden;

  box-sizing: border-box;
  height: ${c.STATISTICS_BAR_HEIGHT};

  font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
  font-size: ${c.GLOBAL_DEFAULT_FONT_SIZE};
  line-height: ${c.STATISTICS_BAR_HEIGHT};
  color: ${c.STATISTICS_TITLE_COLOR};
  text-align: center;

  background-color: ${({ color }) => color};

  transition: width linear ${c.STATISTICS_CELL_MORPH_TIME}ms;
`;

interface StackedBarsProps {
    data: StackedBarsData;
    height?: number;
}

const oneBarHeight = c.STATISTICS_BAR_HEIGHT_PX + c.STATISTICS_BAR_GAP_PX;

export const StackedBars = ({ data, height }: StackedBarsProps) => {
    const rowsCount = useMemo(() => {
        const columns = Math.ceil(data.length / Math.floor(height / oneBarHeight));
        return height ? Math.min(data.length, Math.ceil(data.length / columns)) : data.length;
    }, [data.length, height]);

    return (
        <BarsWrapper rowsCount={rowsCount}>
            {data.map((b) => {
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
                );
            })}
        </BarsWrapper>
    );
};
