FROM openjdk:21-jdk-slim

# jenkins uses regex for replace jar file version in next line before build. Check jenkins branch on change
ADD https://download.lsfusion.org/java/lsfusion-server-5.2-SNAPSHOT.jar server.jar

# java.lang.UnsatisfiedLinkError: /usr/local/openjdk-21/lib/libfontmanager.so: libfreetype.so.6: cannot open shared object file: No such file or directory
# java.lang.NoClassDefFoundError: Could not initialize class sun.awt.X11FontManager
# https://stackoverflow.com/a/58765562/13017592
# https://github.com/docker-library/openjdk/pull/235#issuecomment-424466077
RUN apt-get update && apt-get install -y fontconfig libfreetype6 && rm -rf /var/lib/apt/lists/*

WORKDIR /lsfusion

EXPOSE 7652

CMD ["java",  "-cp", ".:./*:../server.jar", "lsfusion.server.logics.BusinessLogicsBootstrap"]