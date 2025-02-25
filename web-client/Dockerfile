FROM tomcat:9-jdk21

# jenkins use regex for replace war file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-client-5.2-SNAPSHOT.war ./webapps/ROOT.war
#COPY target/lsfusion-client-5.2-SNAPSHOT.war ./webapps/ROOT.war