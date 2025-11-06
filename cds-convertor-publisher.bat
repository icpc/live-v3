SET PORT=8080
SET CONFIG_DIR=config/icpc-wf/2020/
SET CREDS_FILE=creds.json
SET WIDGET_POSITIONS_FILE=widget-positions.json
SET VISUAL_CONFIG=visual-config.json

java -jar artifacts/cds-converter-dev.jar server ^
    --port=%PORT% ^
    --config-directory=%CONFIG_DIR% ^
    --creds=%CREDS_FILE% ^
    --publisher-interval 20 ^
    --publish /reactions/contestInfo.json:public/contestInfo.json ^
    --publish /reactions/runs.json:public/runs.json ^
    --publish-command "cmd /c publisher-ftp.bat"
