FROM openjdk:21-jdk

ARG FILENAME

COPY ${FILENAME} server.jar

WORKDIR /lsfusion

EXPOSE 7652

# Moved to enrypoint to be able to pass JAVA_OPTS, leaving exec-form
COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]