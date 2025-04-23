FROM openjdk:21-jdk-slim

# jenkins uses regex for replace jar file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-server-6.0.jar server.jar

WORKDIR /lsfusion

EXPOSE 7652

CMD ["java",  "-cp", ".:./*:../server.jar", "lsfusion.server.logics.BusinessLogicsBootstrap"]