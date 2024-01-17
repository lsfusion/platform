set BUILD_DIR=%~dp0

call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false -P assemble

cd ../../erp
call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false

rem собираем модули, которые не версионируются, чтобы были актуальные jar'ки
cd ../build
call mvn clean install -P assemble

cd ../build
call mvn clean install -P assemble


cd %BUILD_DIR%
call mvn clean install -P assemble
