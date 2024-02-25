## How to run cloud-instance.
1. Have a machine with docker and docker-compose.
2. Create a folder, put `docker-compose.yml` and `.env` into it.
3. (for now) Package is not public, so you should get access to github read:packages on target machine.
4. `.env` should look like this
   ```
   ID = ...
   PASSWORD = 
   ```
5. `docker-compose.yml` should look like this
   ```
   version: "2.1"
   services:
     code-server:
       image: ghcr.io/icpc/live-v3-instance:latest
       container_name: "live-${ID}"
       environment:
         - PUID=0
         - PGID=0
         - TZ=Etc/UTC
         - PASSWORD=${PASSWORD}
         - SUDO_PASSWORD=${PASSWORD}
         - CS_DISABLE_PROXY=1
       ports:
         - "8443:8443" # codeserver
         - "8080:8080" # overlayer
         - "9001:9001" # supervisord control panel
       volumes:
         - ./config:/config
         - ./workspace:/workspace
         - ./.env:/.env
       restart: unless-stopped
   ```
6. Opt. use traefik to get normal domain names, look at docker-compose.yml in this repo.
7. docker-compose up -d

## TODO
[ ] Make this package public.
[ ] Change something, so we don't have to pass .env file, and only supply password. This is currently here for control panel, because code-server will steal all the environment variables.
[ ] Create a reasonable admin panel.
[ ] Make an easy-to-run example for Amazon ECS.
