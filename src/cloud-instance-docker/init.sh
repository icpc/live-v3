#!/bin/bash

rsync -av --ignore-existing /app/workspace/ /workspace/

exec /usr/bin/supervisord
