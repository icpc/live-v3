import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import bg from "../../assets/images/bg.jpeg";
import Advertisement from "../molecules/widgets/Advertisement";

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

const WidgetWrap = styled.div.attrs(
    ({ left, bottom, width, height }) => {
        return { style: {
            left: left+"px",
            bottom: bottom+"px",
            width: width+"px",
            height: height+"px"
        } };
    }
)`
  position: absolute;
  overflow: hidden;
`;

const MainLayoutWrap = styled.div`
  width: 1920px;
  height: 1080px;
  background: url(${bg});
`;

const WIDGETS = {
    AdvertisementWidget: Advertisement
};

export const MainLayout = () => {
    const widgets = useSelector(state => state.widgets.widgets);
    return <MainLayoutWrap>
        {Object.values(widgets).map((obj) => {
            const Widget = WIDGETS[obj.type];
            return <WidgetWrap
                key={obj.widgetId}
                left={obj.location.positionX}
                bottom={obj.location.positionY}
                width={obj.location.sizeX}
                height={obj.location.sizeY}
            >
                <Widget widgetData={obj}/>
            </WidgetWrap>;
        })})
    </MainLayoutWrap>;
};

export default MainLayout;
