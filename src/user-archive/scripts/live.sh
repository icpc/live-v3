#!/bin/env bash

PORT=8080
CONFIG_DIR=config
CREDS_FILE=creds.json
ANALYTICS_FILE=analytics-en.json
VISUAL_CONFIG=visual-config.json

java -jar live-v3.jar \
    --port=$PORT \
    --config-directory=${CONFIG_DIR} \
    --creds=${CREDS_FILE} \
    --analytics-template=${ANALYTICS_FILE} \
    --visual-config=${VISUAL_CONFIG}
