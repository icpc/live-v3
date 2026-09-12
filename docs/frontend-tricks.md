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
- `http://host/overlay?onlyWidgets=teamViewBottomRight,teamViewBottomLeft`  
   show only bottom team views

## visualConfigScene

Visual config can declare named partial configs in the `SCENES` field, and you can pick them from the url
with the `visualConfigScene` query param. That's a way to keep several looks of the same contest in one
config file, instead of pasting big blobs into `forceVisualConfig` in every OBS browser source.

Scenes are applied after the visual config file, but before `forceVisualConfig`, so a value set in
`forceVisualConfig` still wins over the selected scenes. 

Scenes are chainable: a scene can declare `SCENES` of its own, and dot separated names in the query param
select a chain of them. Several chains can be applied at once, comma separated, left to right.

Example visual config:

```jsonc
{
    "CONTEST_COLOR": "#4C83C3",
    "SCENES": {
        "dark": {
            "GLOBAL_BACKGROUND_COLOR": "#000000",
            "SCENES": {
                "leftQueue": {
                    "WIDGET_POSITIONS": {
                        "queue": { "positionX": 0, "positionY": 0, "sizeX": 384, "sizeY": 1080 }
                    }
                }
            }
        },
        "noCaption": {
            "CONTEST_CAPTION": ""
        }
    }
}
```

Examples:
- `http://host/overlay?visualConfigScene=dark`
   black background
- `http://host/overlay?visualConfigScene=dark.leftQueue`
   black background and the queue moved to the left
- `http://host/overlay?visualConfigScene=dark,noCaption`
   black background and no caption