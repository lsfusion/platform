FROM tomcat:9.0.39-jdk11-openjdk

# jenkins use regex for replace war file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-client-4.0-beta3.war /usr/local/tomcat/webapps/ROOT.war

WORKDIR /usr/local/tomcat

COPY entrypoint.sh entrypoint.sh

EXPOSE 8080

RUN chmod +x entrypoint.sh

ENTRYPOINT "/usr/local/tomcat/entrypoint.sh"
