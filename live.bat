SET PORT=8080
SET CONFIG_DIR=config/atcoder/wtf22-day2/
SET CREDS_FILE=creds.json
SET WIDGET_POSITIONS_FILE=widget-positions.json
SET VISUAL_CONFIG=visual-config.json

java -jar artifacts/live-v3-dev.jar ^
    --port=%PORT% ^
    --config-directory=%CONFIG_DIR% ^
    --creds=%CREDS_FILE% 
#    ^
#    --widget-positions=%WIDGET_POSITIONS_FILE% ^
#    --visual-config=%VISUAL_CONFIG%
