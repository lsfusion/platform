set BUILD_DIR=%~dp0

cd ../../erp
call mvn clean install

cd %BUILD_DIR%
call mvn clean install -P assemble
