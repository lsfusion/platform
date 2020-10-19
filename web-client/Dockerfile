FROM openjdk:11.0.8-jdk

RUN apt-get update; \
    apt-get install -y --no-install-recommends \
            apt-transport-https \
            ca-certificates \
            ; \
# install lsfusion2-client from the private repository
    wget --quiet -O - https://download.lsfusion.org/apt/GPG-KEY-lsfusion | apt-key add -; \
    echo deb https://download.lsfusion.org/apt all main > /etc/apt/sources.list.d/lsfusion.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
            lsfusion4-client \
            ; \
# cleanup
    rm /usr/share/lsfusion4-client/conf/Catalina/localhost/ROOT.xml; \
    rm -rf /var/lib/apt/lists/*;

COPY entrypoint-client.sh entrypoint.sh

EXPOSE 8080
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "/entrypoint.sh" ]
