import React from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import bg from "../../assets/images/bg.jpeg";
import { WIDGET_TRANSITION_TIME } from "../../config";
import Advertisement from "../organisms/widgets/Advertisement";
import Pictures from "../organisms/widgets/Pictures";
import Queue from "../organisms/widgets/Queue";
import Scoreboard from "../organisms/widgets/Scoreboard";
import Ticker from "../organisms/widgets/Ticker";
import Statistics from "../organisms/widgets/Statistics";

const fadeIn = keyframes`
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
`;

const fadeOut = keyframes`
  from {
    opacity: 1;
  }

  to {
    opacity: 0;
  }
`;

const WidgetWrap = styled.div.attrs(
    ({ left, top, width, height }) => {
        return { style: {
            left: left+"px",
            top: top+"px",
            width: width+"px",
            height: height+"px"
        } };
    }
)`
  position: absolute;
  overflow: hidden;
  animation: ${props => props.animation} ${WIDGET_TRANSITION_TIME}ms linear;
`;

const MainLayoutWrap = styled.div`
  width: 1920px;
  height: 1080px;
  background: url(${bg});
`;

const transitionProps = {
    entering: { animation: fadeIn },
    entered:  {  },
    exiting:  { animation: fadeOut },
    exited:  { },
};

const WIDGETS = {
    AdvertisementWidget: Advertisement,
    ScoreboardWidget: Scoreboard,
    QueueWidget: Queue,
    PictureWidget: Pictures,
    TickerWidget: Ticker,
    StatisticsWidget: Statistics
};

export const MainLayout = () => {
    const widgets = useSelector(state => state.widgets.widgets);
    return <MainLayoutWrap>
        <TransitionGroup component={null}>
            {Object.values(widgets).map((obj) => {
                const Widget = WIDGETS[obj.type];
                if(Widget === undefined) {
                    return null;
                }
                return <Transition key={obj.widgetId} timeout={Widget.overrideTimeout ?? WIDGET_TRANSITION_TIME}>
                    {state =>
                        state !== "exited" && <WidgetWrap
                            left={obj.location.positionX}
                            top={obj.location.positionY}
                            width={obj.location.sizeX}
                            height={obj.location.sizeY}
                            {...(transitionProps[state] && !Widget.ignoreTransition)}
                        >
                            <Widget widgetData={obj} transitionState={state}/>
                        </WidgetWrap>
                    }
                </Transition>;
            })}
        </TransitionGroup>
    </MainLayoutWrap>;
};

export default MainLayout;
