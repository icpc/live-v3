#!/bin/bash

rsync -av --ignore-existing /app/workspace/ /workspace/

/app/code-server/bin/code-server --install-extension formulahendry.code-runner

exec /usr/bin/supervisord
