import React, { useEffect, useState } from "react";
import styled from "styled-components";
import c from "../../../config";
import { useAppSelector } from "@/redux/hooks";
import { ContestantInfo } from "../../molecules/info/ContestantInfo";
import { OptimismLevel } from "@shared/api";

type ScoreboardWrapProps = {
    top: string,
    nrows: number
}

const ScoreboardWrap = styled.div.attrs<ScoreboardWrapProps>(({ top }) => (
    { style: { top } }
))<ScoreboardWrapProps>`
  position: absolute;

  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(${props => props.nrows}, 1fr);
  gap: ${c.TICKER_SCOREBOARD_GAP};

  width: 100%;
  height: 100%;

  transition: top ${c.TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME}ms ease-in-out;

  /* align-items: center; */
`;

const TickerScoreboardContestantInfo = styled(ContestantInfo)`
    height: ${c.TICKER_SCOREBOARD_CONTESTANT_INFO_HEIGHT};
`;

export const Scoreboard = ({ tickerSettings, state }) => {
    const { from, to, periodMs } = tickerSettings;
    const [row, setRow] = useState(0);
    const order = useAppSelector((state) => state.scoreboard[OptimismLevel.normal].order.slice(from-1, to));
    const nrows = Math.ceil(order.length / 4);
    useEffect(() => {
        if(state !== "entering" && order.length > 0) {
            const interval = setInterval(() => {
                if (state !== "exiting") {
                    setRow((row) => {
                        return (row + 1) % nrows;
                    });
                }
            }, (periodMs - c.TICKER_SCROLL_TRANSITION_TIME) / nrows / c.TICKER_SCOREBOARD_REPEATS + 1);
            return () => clearInterval(interval);
        }
    }, [nrows, periodMs, state, order.length]);

    // This fugliness is needed to scroll the scoreboard
    return (
        <ScoreboardWrap nrows={nrows*2} top={`calc(${-row * 100 }% - ${row * 2}px)`}>
            {order.map((teamId) => (
                <TickerScoreboardContestantInfo key={teamId} teamId={teamId} useBG={false}/>
            ))}
        </ScoreboardWrap>
    );
};

export default Scoreboard;
