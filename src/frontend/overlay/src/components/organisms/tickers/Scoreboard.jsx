import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import c from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import {ContestantInfo} from "../../molecules/info/ContestantInfo";


const ScoreboardWrap = styled.div.attrs(({ top }) => (
    { style: { top } }
))`
  display: grid;
  //align-items: center;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(${props => props.nrows}, 1fr);
  height: 100%;
  width: 100%;
  gap: 2px;
  position: absolute;
  transition: top ${c.TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME}ms ease-in-out;
`;

const TickerScoreboardContestantInfo = styled(ContestantInfo)`
    height: 48px;
`;

export const Scoreboard = ({ tickerSettings, state }) => {
    const { from, to, periodMs } = tickerSettings;
    const [row, setRow] = useState(0);
    const rows = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].rows.slice(from-1, to));
    const nrows = Math.ceil(rows.length / 4);
    useEffect(() => {
        if(state !== "entering" && rows.length > 0) {
            const interval = setInterval(() => {
                if (state !== "exiting") {
                    setRow((row) => {
                        return (row + 1) % nrows;
                    });
                }
            }, (periodMs - c.TICKER_SCROLL_TRANSITION_TIME) / nrows / c.TICKER_SCOREBOARD_REPEATS + 1);
            return () => clearInterval(interval);
        }
    }, [nrows, periodMs, state, rows.length]);

    // This fugliness is needed to scroll the scoreboard
    return (
        <ScoreboardWrap nrows={nrows*2} top={`calc(${-row * 100 }% - ${row * 2}px)`}>
            {rows.map((row) => (
                <TickerScoreboardContestantInfo key={row.teamId} teamId={row.teamId}/>
            ))}
        </ScoreboardWrap>
    );
};

export default Scoreboard;

