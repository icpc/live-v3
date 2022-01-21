import React from "react";
import styled from "styled-components";
import bg from "../../assets/bg.jpeg";
import Scoreboard from "../molecules/Scoreboard";
import Status from "../molecules/Status";
import Ticker from "../molecules/Ticker";

const TickerWrap = styled.div`
  position: absolute;
  width: 1920px;
  height: 44px;
  left: 0;
  top: 1029px;

  background: #C4C4C4;
`;

const StatusWrap = styled.div`
  position: absolute;
  width: 511px;
  height: 616px;
  left: 31px;
  top: 392px;

  background: #C4C4C4;
`;

const ScoreboardWrap = styled.div`
  position: absolute;
  width: 1300px;
  height: 942px;
  left: 589px;
  top: 66px;
  overflow: hidden;
  //background: rgba(196, 196, 196, 0.53);
`;

const MainLayoutWrap = styled.div`
  width: 1920px;
  height: 1080px;
  background: url(${bg});
`


export const MainLayout = () => {
    return <MainLayoutWrap>
        <ScoreboardWrap><Scoreboard/></ScoreboardWrap>
        <StatusWrap><Status/></StatusWrap>
        <TickerWrap><Ticker/></TickerWrap>
    </MainLayoutWrap>
}

export default MainLayout;