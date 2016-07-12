set BUILD_DIR=%~dp0

call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false

cd ../../erp
call mvn dependency:purge-local-repository -DsnapshotsOnly=true -DreResolve=false

rem собираем модули, которые не версионируются, чтобы были актуальные jar'ки
cd ../build
call mvn clean install

rem erp собирается с платформой версии RC, которая перегружается в профиле assemble. по идее нужно брать версию из профиля assemble, но непонятно, как её оттуда передать
cd ../erp
call mvn clean install

cd %BUILD_DIR%
call mvn clean install -P assemble
