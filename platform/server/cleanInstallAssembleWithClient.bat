set BUILD_DIR=%~dp0

cd %BUILD_DIR%\..
call mvn clean install

cd %BUILD_DIR%
call mvn install -P assemble,pack

cd %BUILD_DIR%\..\desktop-client
call mvn install -P assemble,sign,pack

cd %BUILD_DIR%
