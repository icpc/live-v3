# live-v3

Welcome to the ICPC Live Source Code Repository.

Run as admin

* git config --global core.symlinks true
* git config --global core.autocrlf input

git clone this repository.

Requirements:
* gradle
* node
* jdk
* browser

1. live-v3\backend\gradlew.bat
2. live-v3\backend\gradlew.bat run
3. live-v3\frontend\ npm ci
4. live-v3\frontend\ npm run start
5. open http://localhost:8080/admin

Contest configs are stored at \live-v3\backend\config\ .
Setup configDirectory in \live-v3\backend\config\application.conf .



## Running frontend separate from backend
Frontend takes base url from environment variable `REACT_APP_WEBSOCKET_URL`

On linux:
```
REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/overlay npm run start
```

On Windows:
```
set REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/overlay  
npm run start
```

Previous versions:
* https://github.com/icpc-live
* https://github.com/Aksenov239/icpc-live-v2

Other repos:
* https://github.com/EgorKulikov/acm_profiles script that collects competitive programming historical data for analytical information
* https://github.com/icpc-live/autoanalyst Autoanalyst
* https://github.com/pashkal/obs-video-scheduler OBS Video Scheduler Plugin with web interface
* https://github.com/pmavrin/obs-overlays/tree/master/overlaymaster OBS plugin for shared memory  (dll https://drive.google.com/file/d/1MvCmhlSpftUFC3N2gj0Lv88-ZV2dtnhP)

for more information email Live@icpc.global

General broadcast production schema: https://docs.google.com/document/d/1JcOhmkvbRtG3MLLYUpzVBMqiQOoNpamOz-MvppCgcYk
