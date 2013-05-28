set BUILD_DIR=%~dp0

cd %BUILD_DIR%\..
call mvn clean install

cd %BUILD_DIR%
call assemble

cd %BUILD_DIR%
