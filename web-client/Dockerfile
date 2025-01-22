FROM tomcat:9.0.39-jdk11-openjdk

# jenkins use regex for replace war file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-client-5.2-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
#COPY target/lsfusion-client-5.2-SNAPSHOT.war ./webapps/ROOT.war

WORKDIR /usr/local/tomcat

EXPOSE 8080