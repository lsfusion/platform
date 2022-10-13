FROM openjdk:11.0.8-jdk

# jenkins use regex for replace jar file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-server-5.0-beta0.jar server.jar

WORKDIR /lsfusion

COPY entrypoint.sh /

EXPOSE 7652

RUN chmod +x /entrypoint.sh

ENTRYPOINT "/entrypoint.sh"