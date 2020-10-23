# lsfusion-client-4.0-beta3.war
FROM tomcat:9.0.39-jdk11-openjdk

ADD http://download.lsfusion.org/java/lsfusion-client-4.0-beta3.war /usr/local/tomcat/webapps/ROOT.war

WORKDIR /usr/local/tomcat

COPY entrypoint.sh entrypoint.sh

EXPOSE 8080

RUN chmod +x entrypoint.sh

ENTRYPOINT "/usr/local/tomcat/entrypoint.sh"
