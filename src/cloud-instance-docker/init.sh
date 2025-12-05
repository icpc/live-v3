#!/bin/bash

rsync -av --ignore-existing /app/workspace/ /workspace/

code-server --install-extension formulahendry.code-runner

exec /usr/bin/supervisord
