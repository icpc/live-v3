## forceWidgets

You can pass forceWidgets query params to force which widgets to show on the screen regardless of what's currently
showing from the admin interface.

Examples: 
- `http://host/overlay?forceWidgets=[{%22type%22:%22QueueWidget%22,%22widgetId%22:%22queue%22,%widgetLocationId%22:%22queue%22,%22settings%22:{}}]`  
   show only queue


- `https://host/overlay?forceWidgets=[{"type":"ScoreboardWidget","widgetId":"scoreboard","widgetLocationId":"scoreboard","settings":{"isInfinite":true,"startFromRow":1,"optimismLevel":"normal","group":"all"}}]`

- `https://host/overlay?noStatus&forceWidgets=[{"type":"QueueWidget","widgetId":"queue","widgetLocationId":"customQueue","settings":{"horizontal":%20true}},{"type":"TickerWidget","widgetId":"ticker","widgetLocationId":"customTicker","statisticsId":"ticker","settings":{}}]&forceVisualConfig={"QUEUE_BACKGROUND_COLOR":"%234C83C300","QUEUE_ROW_BACKGROUND":"%234C83C3","QUEUE_TITLE":"","CONTEST_NAME":"","WIDGET_POSITIONS":{"customQueue":{"positionX":16,"positionY":832,"sizeX":1888,"sizeY":168},"customTicker":{"positionX":16,"positionY":1016,"sizeX":1888,"sizeY":48}}}`

## forceVisualConfig

You can pass forces visual config using the forceVisualConfig 

Examples:
- `http://host/overelay?forceVisualConfig=`


## onlyWidgets
You can pass onlyWidgets query params to show only the widgets that are passed in the query params by their id.

Examples:
- `http://host/overlay?onlyWidgets=queue`  
   show only queue
- `http://host/overlay?onlyWidgets=teamview.BOTTOM_RIGHT,teamview.BOTTOM_LEFT`  
   show only bottom teamviews
