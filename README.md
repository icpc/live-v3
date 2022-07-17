# live-v3

Welcome to the ICPC Live Source Code Repository.

# Run release version

* Download release from https://github.com/icpc/live-v3/releases
* Create contest config files
    * [Example for CLICS](https://github.com/icpc/live-v3/tree/main/backend/config/archive/rmc21)
    * [Example for PCMS](https://github.com/icpc/live-v3/tree/main/backend/config/archive/nerc-2021)
    * [Example for Codeforces](https://github.com/icpc/live-v3/tree/main/backend/config/archive/vkoshp-junior-2022)
    * [Example for Yandex](https://github.com/icpc/live-v3/tree/main/backend/config/archive/yandex-example/)
    * [See full archive for more examples](https://github.com/icpc/live-v3/tree/main/backend/config/archive)
    * [How to fine tune imported data](https://github.com/icpc/live-v3/blob/main/advanced.json.md)
*

Run `java -jar /path/to/jar/file -port=8080 -P:live.configDirectory=/path/to/config/directory -P:live.credsFile=creds.json`

* Port 8080 is default, if you are okay with it option can be omitted

* Add source to OBS
    * +Source
    * Browser
    * URL http://localhost:8080/overlay?noStatus
    * W H 1920x1080
    * OBS Custom css:

```
#root > div {
background: unset;
}
```

* Use http://localhost:8080/admin in your browser to control overlay

Also, check emulation mode part of development doc for testing.

# Authorisation

For now http basic auth is used. If you try to login with
user, which does not exist, it will be automatically created.
First created receives admin rights. Others only receive them when
someone with admin rights confirms, it's okey.

Consider, if you are okay with passing your passwords using plain HTTP.

If you don't need auth, you can disable it by -P:auth.disabled=true command-line option,
or corresponding property in config file.

# Run in development mode

Requirements:

* gradle
* jdk
* node:16
* browser

Before cloning on Windows configure correct crlf handling

* `git config --global core.symlinks true`

## Developing backend

### To test your changes:

1. Setup configDirectory in `live-v3\backend\config\application.conf`
2. `live-v3\backend\gradlew buildJs`
3. `live-v3\backend\gradlew run`
4. open http://localhost:8000/admin to control overlay
5. open http://localhost:8000/overlay to view result

Beware of separate release config in resources directory if adding new fields to config.

### General backend architecture

Backend is implemented in kotlin as [ktor](https://ktor.io/docs/) server.

Admin api endpoints are `/api/admin`. They are using REST-like conventions for naming,
and mostly implemented in `org.icpclive.admin` package. All data is stored in json-files in
contest config directory for now.

Overlay api endpoints are `/api/overlay`. They are websockets with updates, read by
overlay frontend part. Internally, they are implemented
as [flows](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/)
from kotlinx.coroutines.

Admin and overlay can be hosted over `/admin` and `/overlay` paths as SPA using ktor builtin
SPA hosting.

Contest systems integrations are implemented in `org.icpclive.cds` package. Currently,
CDS, PCMS, Codeforces and Yandex.Contest are supported. Currently, only ICPC mode
is supported.

Basically, to add new contest data provider, you need to implement contest information updates
(start time, list of problems, list of teams, etc.) and runs updates (events like new run, run status changes, partial
and final testing).
Everything else should work automatically in same manner for all CDS sources.

### Emulation mode

If `elmulation.startTime` property is defined, instead of regular updating, cds provider need to download all runs once,
and
they would be timely pushed further automatically. This is useful to check everything on already happened contest.

```
emulation.speed=10
emulation.startTime=2022-04-03 22:50
```

Now, for supporting this in new contest data provider, you need to copypaste code
parsing the option, and starting corresponding services. Hope, it will be fixed later.

## Developing frontend

We have two front end packages:

* overlay - webapp that is rendered in OBS (located in `overlay`)
* admin - admin app for controlling the overlay (located in `admin`)

Install dependencies with `npm ci` in the root path of the project
(see package.json for more details)

### Running frontend separate from backend

If you are running locally replace `<ip>` with `localhost`

#### overlay

Overlay takes base url from environment variable `REACT_APP_WEBSOCKET_URL`  
A path to backend's path for websocket connection.  
Exposed on /api/overlay path

Run this in `overlay` directory to start the development server:  
Linux:

```
REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/api/overlay npm run start
```

Windows:

```
set REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/api/overlay  
npm run start
```

#### admin

Admin panel takes two urls:

* `REACT_APP_BACKEND_URL` - for updating data and talking to the backend (exposed in /api/admin)
* `REACT_APP_WEBSOCKET_URL` - for real time updates of presets and settings (exposed in /api/admin)

Run this in `admin` directory to start the development server:  
Linux:

```
REACT_APP_BACKEND_URL=http://<IP>:8080/api/admin;REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/api/admin npm run start
```

Windows:

```
set REACT_APP_BACKEND_URL=http://<IP>:8080/api/admin  
set REACT_APP_WEBSOCKET_URL=ws://<IP>:8080/api/admin
npm run start
```

# Previous versions:

* https://github.com/icpc-live
* https://github.com/Aksenov239/icpc-live-v2

Other repos:

* https://github.com/EgorKulikov/acm_profiles script that collects competitive programming historical data for
  analytical information
* https://github.com/icpc-live/autoanalyst Autoanalyst
* https://github.com/pashkal/obs-video-scheduler OBS Video Scheduler Plugin with web interface
* https://github.com/pmavrin/obs-overlays/tree/master/overlaymaster OBS plugin for shared memory  (
  dll https://drive.google.com/file/d/1MvCmhlSpftUFC3N2gj0Lv88-ZV2dtnhP)

for more information email Live@icpc.global

General broadcast production schema: https://docs.google.com/document/d/1JcOhmkvbRtG3MLLYUpzVBMqiQOoNpamOz-MvppCgcYk
