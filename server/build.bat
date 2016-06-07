set BUILD_DIR=%~dp0

call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false

cd ../../erp
call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false
call mvn clean install

cd %BUILD_DIR%
call mvn clean install -P assemble
