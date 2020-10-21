# lsfusion4-client=4.0-beta3-1
FROM openjdk:11.0.8-jdk

RUN apt-get update; \
    apt-get install -y --no-install-recommends \
            apt-transport-https \
            ca-certificates \
            ; \
# install lsfusion-client from the private repository
    wget --quiet -O - https://download.lsfusion.org/apt/GPG-KEY-lsfusion | apt-key add -; \
    echo deb https://download.lsfusion.org/apt all main > /etc/apt/sources.list.d/lsfusion.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
            lsfusion4-client=4.0-beta3-1 \
            ; \
# cleanup
    rm /usr/share/lsfusion4-client/conf/Catalina/localhost/ROOT.xml; \
    rm -rf /var/lib/apt/lists/*;

WORKDIR /usr/share/lsfusion4-client

COPY entrypoint.sh entrypoint.sh

EXPOSE 8080
RUN chmod +x entrypoint.sh
ENTRYPOINT "/usr/share/lsfusion4-client/entrypoint.sh"
