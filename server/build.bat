set BUILD_DIR=%~dp0

cd ../build
call mvn clean install

cd ../api
call mvn clean install

cd %BUILD_DIR%
call mvn clean install
