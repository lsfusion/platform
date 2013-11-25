set BUILD_DIR=%~dp0

cd %BUILD_DIR%\..
call mvn -U clean install

cd %BUILD_DIR%
call assemble

cd %BUILD_DIR%
