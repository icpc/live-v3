import React from "react";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { Keyframes, keyframes } from "styled-components";
import bg from "../../assets/images/bg.png";
import c from "../../config";
import { DEBUG } from "@/consts";
import { useAppSelector } from "@/redux/hooks";
import { useQueryParams } from "@/utils/query-params";
import { StatusLightBulbs } from "../organisms/status/StatusLightBulbs";
import Advertisement from "../organisms/widgets/Advertisement";
import Pictures from "../organisms/widgets/Pictures";
import Svg from "../organisms/widgets/Svg";
import Queue from "../organisms/widgets/Queue";
import Scoreboard from "../organisms/widgets/Scoreboard";
import Ticker from "../organisms/widgets/Ticker";
import Statistics from "../organisms/widgets/Statistics";
import { TeamView } from "../organisms/widgets/TeamView";
import Videos from "../organisms/widgets/Videos";
// import PVP from "../organisms/widgets/PVP";
import FullScreenClock from "../organisms/widgets/FullScreenClock";
import Locator from "../organisms/widgets/Locator";
import { Widget } from "@shared/api";

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

type WidgetWrapProps = {
    left: number | string,
    top: number | string,
    width: number | string,
    height: number | string,
    shouldCrop?: boolean,
    zIndex: number,
    animation: Keyframes
}

const WidgetWrap = styled.div.attrs<WidgetWrapProps>(
    ({ left, top, width, height }) => {
        return { style: {
            left: left + "px",
            top: top + "px",
            width: width + "px",
            height: height + "px",
        } };
    }
)<WidgetWrapProps>`
  position: absolute;
  z-index: ${({ zIndex }) => zIndex};

  overflow: ${({ shouldCrop = true }) => shouldCrop ? "hidden" : ""};

  animation: ${props => props.animation} ${c.WIDGET_TRANSITION_TIME}ms linear;
  animation-fill-mode: forwards;
`;

const MainLayoutWrap = styled.div`
  width: ${c.SCREEN_WIDTH}px;
  height: ${c.SCREEN_HEIGHT}px;
  background: ${c.BACKGROUND ?? (DEBUG ? `url(${bg})` : undefined)};
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
    SvgWidget: Svg,
    VideoWidget: Videos,
    TickerWidget: Ticker,
    StatisticsWidget: Statistics,
    TeamViewWidget: TeamView,
    // TeamPVPWidget: PVP, // Not actually a widget in backend.
    FullScreenClockWidget: FullScreenClock,
    TeamLocatorWidget: Locator
};

const useWidgets = () => {
    const queryParams = useQueryParams();
    if(queryParams.has("forceWidgets")) {
        console.info("forceWidgets=", queryParams.get("forceWidgets"));
        return JSON.parse(queryParams.get("forceWidgets")) as Record<Widget["widgetId"], Widget>;
    } else {
        return useAppSelector(state => state.widgets.widgets);
    }
};

export const MainLayout = () => {
    const widgets = useWidgets();
    const params = useQueryParams();
    return <MainLayoutWrap>
        <StatusLightBulbs compact={true}/>
        <TransitionGroup component={null}>
            {Object.values(widgets).map((obj) => {
                const Widget = WIDGETS[obj.type];
                if (Widget === undefined) {
                    return null;
                }
                if (obj.type === "TeamLocatorWidget" && obj.settings?.scene !== (params.get("scene") || undefined)) {
                    // FIXME: feature for multi vmix sources coordination. Should be moved to the Widget class
                    return null;
                }
                if (params.get("scene") && obj.type !== "TeamLocatorWidget") {
                    return null;
                }
                if (params.get("onlyWidgets") && !params.get("onlyWidgets").split(",").includes(obj.widgetId)) {
                    return null;
                }
                return <Transition key={obj.widgetId} timeout={Widget.overrideTimeout ?? c.WIDGET_TRANSITION_TIME}>
                    {state =>
                        state !== "exited" && <WidgetWrap
                            data-widget-id={obj.widgetId}
                            left={obj.location.positionX}
                            top={obj.location.positionY}
                            width={obj.location.sizeX}
                            height={obj.location.sizeY}
                            shouldCrop={Widget.shouldCrop}
                            zIndex={Widget.zIndex ?? 0}
                            {...(!Widget.ignoreAnimation && transitionProps[state])}
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
