FROM openjdk:11.0.8-jdk

ARG FILENAME

COPY ${FILENAME} server.jar

WORKDIR /lsfusion

EXPOSE 7652

CMD ["java",  "-cp", ".:./*:../server.jar", "lsfusion.server.logics.BusinessLogicsBootstrap"]