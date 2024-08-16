export type WidgetSetting = {
    path: string;
    settings?: any;
}

export type AnalyticsSettings = {
    messageId: string;
    makeFeaturedType: string;
}

export type TestLayout = {
    widgets: WidgetSetting[];
    analytics?: AnalyticsSettings;
    displayDelay?: number;
}

export type TestConfig = {
    path: string;
    layouts?: TestLayout[];
};
