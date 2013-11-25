set BUILD_DIR=%~dp0

call build

REM to also download client
REM call mvn -U org.apache.maven.plugins:maven-dependency-plugin:2.8:copy -Dartifact=lsfusion.platform:desktop-client:1.2.0-SNAPSHOT:pack.gz:assembly -DoutputDirectory=target

call mvn -U org.apache.maven.plugins:maven-dependency-plugin:2.8:copy -Dartifact=lsfusion.platform:web-client:1.2.0-SNAPSHOT:war -DoutputDirectory=target
