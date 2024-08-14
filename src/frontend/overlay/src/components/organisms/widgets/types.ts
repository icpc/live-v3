import { Widget } from "@shared/api";
import { FC } from "react";

export type OverlayWidgetProps<W extends Widget> = {
    widgetData: W;
    transitionState: string;
};

export type OverlayWidgetC<W extends Widget> = FC<OverlayWidgetProps<W>> & {
    ignoreAnimation?: boolean;
    overrideTimeout?: number;
}
