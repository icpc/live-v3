FROM lscr.io/linuxserver/code-server:latest

ENV DEFAULT_WORKSPACE /workspace

RUN apt-get update && apt-get install -y supervisor openjdk-17-jdk rsync unzip

COPY run.sh /app/workspace/run.sh
COPY stop.sh /app/workspace/stop.sh
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY init.sh /init.sh

ARG USER_ARCHIVE
COPY ${USER_ARCHIVE} /app/workspace/live-v3.zip
RUN unzip /app/workspace/live-v3.zip -d /app/workspace

CMD ["/init.sh"]

