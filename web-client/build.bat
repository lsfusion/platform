set BUILD_DIR=%~dp0

cd %BUILD_DIR%\..\build
call mvn clean install

cd %BUILD_DIR%\..\api
call mvn clean install

cd %BUILD_DIR%\..\desktop-client
call mvn clean install

cd %BUILD_DIR%
call mvn clean install