FROM openjdk:11.0.8-jdk

# jenkins use regex for replace jar file version in next line before build. Check jenkins branch on change

ADD https://download.lsfusion.org/java/lsfusion-server-5.2-SNAPSHOT.jar server.jar
#COPY target/lsfusion-server-5.2-SNAPSHOT.jar server.jar

WORKDIR /lsfusion

EXPOSE 7652

CMD ["java",  "-cp", ".:./*:../server.jar", "lsfusion.server.logics.BusinessLogicsBootstrap"]