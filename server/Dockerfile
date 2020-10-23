FROM openjdk:11.0.8-jdk

ARG PACKAGE

COPY $PACKAGE lsfusion/server.jar

WORKDIR /lsfusion

COPY entrypoint.sh entrypoint.sh

EXPOSE 7652

RUN chmod +x entrypoint.sh

ENTRYPOINT "/lsfusion/entrypoint.sh"