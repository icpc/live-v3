# syntax=docker/dockerfile:1

FROM alpine:3.21 AS extract

RUN apk add --no-cache unzip

ARG USER_ARCHIVE
COPY ${USER_ARCHIVE} /tmp/live-v3.zip

RUN mkdir -p /extracted && \
    unzip /tmp/live-v3.zip -d /extracted

FROM lscr.io/linuxserver/code-server:latest

ENV DEFAULT_WORKSPACE=/workspace

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        supervisor openjdk-21-jdk rsync && \
    rm -rf /var/lib/apt/lists/*

COPY run.sh /app/workspace/run.sh
COPY stop.sh /app/workspace/stop.sh
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY init.sh /init.sh

COPY --from=extract /extracted/ /app/workspace/

CMD ["/init.sh"]
