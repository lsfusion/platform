FROM openjdk:11.0.8-jdk

ARG APT_PACKAGE
ARG APT_VERSION
ENV package=$APT_PACKAGE

RUN apt-get update; \
    apt-get install -y --no-install-recommends \
            apt-transport-https \
            ca-certificates \
            ; \
# install lsfusion-server from the private repository
    wget --quiet -O - https://download.lsfusion.org/apt/GPG-KEY-lsfusion | apt-key add -; \
    echo deb https://download.lsfusion.org/apt all main > /etc/apt/sources.list.d/lsfusion.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
            $APT_PACKAGE=$APT_VERSION \
            ; \
# cleanup
    rm  /usr/share/$APT_PACKAGE/conf/settings.properties; \
    rm -rf /var/lib/apt/lists/*;

WORKDIR /usr/share/$APT_PACKAGE

COPY entrypoint.sh entrypoint.sh

EXPOSE 7652
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "/usr/share/$package/entrypoint.sh" ]