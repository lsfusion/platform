set BUILD_DIR=%~dp0

cd %BUILD_DIR%\..
mvn clean install

cd %BUILD_DIR%
mvn install -P assemble,pack

cd %BUILD_DIR%\client
mvn install -P assemble,sign,pack

cd %BUILD_DIR%\fullclient
mvn install -P assemble,sign,pack
