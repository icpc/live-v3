FROM lscr.io/linuxserver/code-server:latest

ENV DEFAULT_WORKSPACE /workspace

RUN apt-get update && apt-get install -y supervisor openjdk-17-jdk rsync

COPY live.sh /app/workspace/live.sh
COPY run.sh /app/workspace/run.sh
COPY stop.sh /app/workspace/stop.sh
COPY creds.json /app/workspace/creds.json
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf


COPY init.sh /init.sh

CMD ["/init.sh"]

