# NEF Ribbon & Cube
This file contains frontend tricks URLs used at the 2025 Northern Eurasia Finals to provide useful information for contestants, judges and the technical committee. For all examples you must use the provided [visual-config.json](./visual-config.json) file.

## Tickers in the Ribbon
URL: ```http://<overlayUrl>/overlay?noStatus&forceWidgets=[{"type":"TickerWidget","widgetId":"ticker0","widgetLocationId":"ticker0","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker1","widgetLocationId":"ticker1","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker2","widgetLocationId":"ticker2","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker3","widgetLocationId":"ticker3","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker4","widgetLocationId":"ticker4","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker5","widgetLocationId":"ticker5","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker6","widgetLocationId":"ticker6","statisticsId":"ticker"},{"type":"TickerWidget","widgetId":"ticker7","widgetLocationId":"ticker7","statisticsId":"ticker"}]```

## Scoreboard in the Cube
URL: ```http://<overlayUrl>/overlay?noStatus&forceWidgets=[{"type":"ScoreboardWidget","widgetId":"cubeScoreboard","widgetLocationId":"cubeScoreboard","settings":{"isInfinite":true,"startFromRow":1,"optimismLevel":"normal","group":"all"}}]```

## Clocks in the Cube
Contest Time URL: ```http://<overlayUrl>/overlay?noStatus&forceWidgets=[{"type":"FullScreenClockWidget","widgetId":"cubeClock","widgetLocationId":"cubeClock","statisticsId":"clock","settings":{"clockType":"standard","showSeconds":true}}]```

Countdown Time URL: ```http://<overlayUrl>/overlay?noStatus&forceWidgets=[{"type":"FullScreenClockWidget","widgetId":"cubeClock","widgetLocationId":"cubeClock","statisticsId":"clock","settings":{"clockType":"countdown","showSeconds":true}}]```

Global Time URL: ```http://<overlayUrl>/overlay?noStatus&forceWidgets=[{"type":"FullScreenClockWidget","widgetId":"cubeClock","widgetLocationId":"cubeClock","statisticsId":"clock","settings":{"clockType":"global","showSeconds":true}}]```

To make it look more impressive, you can place the clock on top of the scoreboard in OBS and set the clock's opacity to around 25%.
