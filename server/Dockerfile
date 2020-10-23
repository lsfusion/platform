# lsfusion-server-4.0-beta3.jar
FROM openjdk:11.0.8-jdk

ADD http://download.lsfusion.org/java/lsfusion-server-4.0-beta3.jar lsfusion/server.jar

WORKDIR /lsfusion

COPY entrypoint.sh entrypoint.sh

EXPOSE 7652

RUN chmod +x entrypoint.sh

ENTRYPOINT "/lsfusion/entrypoint.sh"