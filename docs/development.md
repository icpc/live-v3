# Run in development mode

Requirements:

* gradle
* jdk
* node:20 with pnpm
* browser

Before cloning on Windows configure correct crlf handling

* `git config --global core.autocrlf false`
* `git config --global core.symlinks true`

## Developing backend

### To test your changes:

1. `live-v3\gradlew :backend:run -Plive.dev.contest=nerc-onsite-2020`
    * Or run one of configurations from IDEA stored in .run
2. open http://localhost:8080/admin to control overlay
3. open http://localhost:8080/overlay to view result

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
CLICS, PCMS, Codeforces and Yandex.Contest, Ejudge and KRSU are supported. Only ICPC mode
is supported at the moment.

Basically, to add new contest data provider, you need to implement contest information updates
(start time, list of problems, list of teams, etc.) and runs updates (events like new run, run status changes, partial
and final testing). In simple cases, if you reload all data every time, it can be useful to inherit from FullReloadContestDataSource.

Everything else should work automatically in the same manner for all CDS sources.

## Developing frontend

We have two front end packages:

* overlay - webapp that is rendered in OBS (located in `overlay`)
* admin - admin app for controlling the overlay (located in `admin`)

Install dependencies with `pnpm install` in the src/frontend path of the project
(see src/frontend/package.json and src/frontend/pnpm-workspaces for more details)

### Running frontend separate from backend

If you are running locally replace `<ip>` with `localhost`

#### overlay

Overlay takes base url from environment variable `VITE_WEBSOCKET_URL`  
A path to backend's path for websocket connection.  
Exposed on /api/overlay path

Run this in `overlay` directory to start the development server:  
Linux:

```
VITE_WEBSOCKET_URL=ws://<IP>:8080/api/overlay npm run start
```

Windows:

```
set VITE_WEBSOCKET_URL=ws://<IP>:8080/api/overlay  
npm run start
```

#### admin

Admin panel takes two urls:

* `VITE_BACKEND_URL` - for updating data and talking to the backend (exposed in /api/admin)
* `VITE_WEBSOCKET_URL` - for real time updates of presets and settings (exposed in /api/admin)

Run this in `admin` directory to start the development server:  
Linux:

```
VITE_BACKEND_URL=http://<IP>:8080/api/admin;VITE_WEBSOCKET_URL=ws://<IP>:8080/api/admin npm run start
```

Windows:

```
set VITE_BACKEND_URL=http://<IP>:8080/api/admin  
set VITE_WEBSOCKET_URL=ws://<IP>:8080/api/admin
npm run start
```

# Run tests

Create release in artifacts, then run:
```bash
npm ci
npx playwright install --with-deps
npx playwright test
```
