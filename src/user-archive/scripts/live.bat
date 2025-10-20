SET PORT=8080
SET CONFIG_DIR=config
SET CREDS_FILE=creds.json
SET ANALYTICS_FILE=analytics-en.json
SET VISUAL_CONFIG=visual-config.json

java -jar live-v3.jar ^
    --port=%PORT% ^
    --config-directory=%CONFIG_DIR% ^
    --creds=%CREDS_FILE% ^
    --analytics-template=%ANALYTICS_FILE% ^
    --visual-config=%VISUAL_CONFIG%
