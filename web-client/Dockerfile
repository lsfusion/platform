FROM tomcat:9.0.39-jdk11-openjdk

ARG PACKAGE

COPY $PACKAGE /usr/local/tomcat/deploy/client.war

WORKDIR /usr/local/tomcat

COPY entrypoint.sh entrypoint.sh

EXPOSE 8080

RUN chmod +x entrypoint.sh

ENTRYPOINT "/usr/local/tomcat/entrypoint.sh"
