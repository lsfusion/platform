FROM openjdk:8-jre-alpine3.9

ARG VERSION

WORKDIR /app

ENV JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap

RUN apk add --no-cache bash

COPY target/lsfusion-server-${VERSION}.jar /lsfusion-server.jar
COPY docker-entrypoint.sh /entrypoint.sh

EXPOSE 7652

ENTRYPOINT [ "/entrypoint.sh" ]
