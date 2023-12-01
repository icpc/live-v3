#!/bin/bash

rsync -av --ignore-existing /app/workspace/ /workspace/

set -a
source .env
set +a

exec /usr/bin/supervisord
