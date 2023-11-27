#!/bin/sh

rsync -av --ignore-existing /app/workspace/ /workspace/

export PASSWORD=$PASSWORD

exec /usr/bin/supervisord
