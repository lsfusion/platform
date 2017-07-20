set BUILD_DIR=%~dp0

rem собираем модули, которые не версионируются, чтобы были актуальные jar'ки
cd ../../build
call mvn clean install

cd ../erp
call mvn clean install

cd %BUILD_DIR%
call mvn clean install -P assemble
