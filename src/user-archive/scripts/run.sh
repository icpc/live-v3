#!/bin/env bash

PORT=8080
CONFIG_DIR=config
CREDS_FILE=creds.json
WIDGET_POSITIONS_FILE=widget-positions.json
VISUAL_CONFIG=visual-config.json

java -jar live-v3.jar \
    --port=$PORT \
    --config-directory=${CONFIG_DIR} \
    --creds=${CREDS_FILE} \
    --widget-positions=${WIDGET_POSITIONS_FILE} \
    --visual-config=${VISUAL_CONFIG}
