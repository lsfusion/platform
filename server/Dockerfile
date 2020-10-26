# lsfusion-server-4.0-beta3.jar
FROM openjdk:11.0.8-jdk

ADD https://download.lsfusion.org/java/lsfusion-server-4.0-beta3.jar server.jar

WORKDIR /lsfusion

COPY entrypoint.sh /

EXPOSE 7652

RUN chmod +x /entrypoint.sh

ENTRYPOINT "/entrypoint.sh"