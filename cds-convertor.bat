SET PORT=8080
SET CONFIG_DIR=config/icpc-northern-eurasia/nwq-2025-2026/
SET CREDS_FILE=creds.json
SET WIDGET_POSITIONS_FILE=widget-positions.json
SET VISUAL_CONFIG=visual-config.json

java -jar artifacts/cds-converter-dev.jar server ^
    --port=%PORT% ^
    --config-directory=%CONFIG_DIR% ^
    --creds=%CREDS_FILE% 
