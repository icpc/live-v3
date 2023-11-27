#!/bin/bash

exec java -jar live.jar --port=8080 --config-directory=config --creds=creds.json > output.log 2> error.log
