FROM openjdk:11.0.8-jdk

RUN apt-get update; \
    apt-get install -y --no-install-recommends \
            apt-transport-https \
            ca-certificates \
            ; \
# install lsfusion4-server from the private repository
    wget --quiet -O - https://download.lsfusion.org/apt/GPG-KEY-lsfusion | apt-key add -; \
    echo deb https://download.lsfusion.org/apt all main > /etc/apt/sources.list.d/lsfusion.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
            lsfusion4-server \
            ; \
# cleanup
    rm  /usr/share/lsfusion4-server/conf/settings.properties; \
    rm -rf /var/lib/apt/lists/*;

WORKDIR /usr/share/lsfusion4-server

COPY entrypoint-server.sh entrypoint.sh

EXPOSE 7652
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "/usr/share/lsfusion4-server/entrypoint.sh" ]