import React from "react";
import { useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import bg from "../../assets/images/bg.jpeg";
import { WIDGET_TRANSITION_TIME } from "../../config";
import Advertisement from "../molecules/widgets/Advertisement";

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
  animation: ${props => props.animation } ${WIDGET_TRANSITION_TIME}ms linear;
  opacity: ${props => props.shown ? "1" : "0" };
`;

const MainLayoutWrap = styled.div`
  width: 1920px;
  height: 1080px;
  background: url(${bg});
`;

const WIDGETS = {
    AdvertisementWidget: Advertisement
};

const transitionProps = {
    entering: { animation: fadeIn },
    entered:  {  },
    exiting:  { animation: fadeOut },
    exited:  { shown: false },
};

export const MainLayout = () => {
    const widgets = useSelector(state => state.widgets.widgets);
    return <MainLayoutWrap>
        <TransitionGroup component={null}>
            {Object.values(widgets).map((obj) => {
                const Widget = WIDGETS[obj.type];
                return <Transition key={obj.widgetId} timeout={WIDGET_TRANSITION_TIME}>
                    {state => {
                        return <WidgetWrap
                            left={obj.location.positionX}
                            bottom={obj.location.positionY}
                            width={obj.location.sizeX}
                            height={obj.location.sizeY}
                            {...transitionProps[state]}
                        >
                            <Widget widgetData={obj}/>
                        </WidgetWrap>;
                    }}
                </Transition>;
            })}
        </TransitionGroup>
    </MainLayoutWrap>;
};

export default MainLayout;
